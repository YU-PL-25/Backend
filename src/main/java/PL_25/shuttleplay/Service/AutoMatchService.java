package PL_25.shuttleplay.Service;


import PL_25.shuttleplay.dto.Matching.AutoMatchRequest;
import PL_25.shuttleplay.dto.Matching.MMRDTO;
import PL_25.shuttleplay.dto.Matching.ProfileDTO;
import PL_25.shuttleplay.Entity.Game.*;
import PL_25.shuttleplay.Repository.*;
import PL_25.shuttleplay.Entity.Game.*;
import PL_25.shuttleplay.Repository.*;
import PL_25.shuttleplay.dto.Matching.AutoMatchRequest;
import PL_25.shuttleplay.dto.Matching.MMRDTO;
import PL_25.shuttleplay.dto.Matching.ProfileDTO;
import PL_25.shuttleplay.Entity.Location;
import PL_25.shuttleplay.Entity.User.MMR;
import PL_25.shuttleplay.Entity.User.NormalUser;
import PL_25.shuttleplay.Entity.User.Profile;
import PL_25.shuttleplay.Util.GeoUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AutoMatchService {

    private final MatchQueueRepository matchQueueRepository;
    private final GameRoomRepository gameRoomRepository;
    private final GameRepository gameRepository;
    private final NormalUserRepository normalUserRepository;
    private final GameHistoryRepository gameHistoryRepository;
    private final GameParticipantRepository gameParticipantRepository;


    // 유효성 검사
    private void validateUsersBeforeMatch(List<Long> userIds, GameRoom currentRoomOrNull) {
        for (Long userId : userIds) {
            boolean inGame = gameParticipantRepository.existsByUser_UserIdAndGame_Status(userId, GameStatus.ONGOING);
            if (inGame) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "userId=" + userId + " 는 현재 게임 중입니다.");
            }
            List<MatchQueueEntry> existing = matchQueueRepository.findByUser_UserIdAndMatchedFalse(userId);
            for (MatchQueueEntry entry : existing) {
                if (entry.getGameRoom() != null &&
                        (currentRoomOrNull == null || !entry.getGameRoom().getGameRoomId().equals(currentRoomOrNull.getGameRoomId()))) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "userId=" + userId + " 는 이미 다른 방에 대기 중입니다.");
                }
            }
        }
    }

   // 통합된 매칭 큐 등록
   public MatchQueueResponse registerToQueue(Long userId, AutoMatchRequest request) {
       NormalUser user = normalUserRepository.findById(userId)
               .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없음"));

       if (request.getLocation() == null ||
               request.getLocation().getCourtName() == null ||
               request.getLocation().getCourtAddress() == null) {
           throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "코트 이름과 주소는 필수입니다.");
       }

       Long roomId = user.getGameRoom() != null ? user.getGameRoom().getGameRoomId() : null;

       if (roomId != null &&
               matchQueueRepository.existsByUser_UserIdAndGameRoom_GameRoomIdAndMatchedFalse(userId, roomId)) {
           throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 해당 게임방에 매칭 등록이 되어있습니다.");
       }

       if (matchQueueRepository.existsByUser_UserIdAndMatchedFalseAndGameRoomIsNotNull(userId)) {
           throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 다른 게임방에 매칭 등록이 되어있습니다.");
       }

       MatchQueueEntry entry = new MatchQueueEntry();
       entry.setUser(user);

       ProfileDTO dto = request.getProfile();
       Profile profile = new Profile();
       profile.setGameType(dto.getGameType());
       profile.setAgeGroup(dto.getAgeGroup());
       profile.setPlayStyle(dto.getPlayStyle());
       entry.setProfile(profile);

       MMRDTO mmrDto = request.getMmr();
       MMR mmr = new MMR();
       mmr.setRating(mmrDto.getRating());
       mmr.setTolerance(mmrDto.getTolerance());
       entry.setMmr(mmr);

       entry.setLocation(request.getLocation());
       entry.setIsPrematched(request.isPreMatch());
       entry.setMatched(false);

       if (request.isPreMatch()) {
           // 사전매칭 큐 등록
           entry.setMatchType(MatchQueueType.QUEUE_PRE);
           entry.setDate(request.getDate());
           entry.setTime(request.getTime());
           entry.setGameRoom(null);
       } else {
           // 현장매칭 큐 등록
           entry.setMatchType(MatchQueueType.QUEUE_LIVE);
           entry.setDate(LocalDate.now());
           entry.setTime(LocalTime.now());

           if (user.getGameRoom() != null) {
               entry.setGameRoom(user.getGameRoom());
           }
       }

       MatchQueueEntry savedEntry = matchQueueRepository.save(entry);
       return new MatchQueueResponse(savedEntry);
   }

    // 매칭 큐 등록 취소
    public void cancelQueueEntry(Long userId) {
    List<MatchQueueEntry> entries = matchQueueRepository.findByUser_UserIdAndMatchedFalse(userId);

        if (entries.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "매칭 큐에 등록된 유저가 없습니다.");
        }

        matchQueueRepository.deleteAll(entries);
    }


    // ========================== 사전매칭 (구장 기준) ==========================
    public Game matchPreCourtFromRoom(Long roomId) {
        GameRoom room = gameRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("게임방을 찾을 수 없습니다."));

        // 이 방에 등록된 QUEUE_LIVE 유저들만 필터링
        List<MatchQueueEntry> queue = matchQueueRepository.findByMatchedFalseAndGameRoom_GameRoomId(roomId).stream()
                .filter(entry -> entry.getMatchType() == MatchQueueType.QUEUE_LIVE)
                .toList();

        List<Long> userIds = queue.stream()
                .map(e -> e.getUser().getUserId())
                .toList();
        validateUsersBeforeMatch(userIds, room);

        // 게임 타입별 그룹핑 (단식/복식)
        Map<String, List<MatchQueueEntry>> groupedByGameType = queue.stream()
                .collect(Collectors.groupingBy(e -> e.getProfile().getGameType()));

        for (Map.Entry<String, List<MatchQueueEntry>> entry : groupedByGameType.entrySet()) {
            String gameType = entry.getKey();
            List<MatchQueueEntry> candidates = entry.getValue();

            // 사용자 수가 충분한지 먼저 검사
            int required = getRequiredMatchCount(gameType);
            if (candidates.size() < required + 1) continue; // 본인 + 상대들

            // 매칭 시도
            for (MatchQueueEntry me : candidates) {
                List<MatchQueueEntry> potentialPartners = candidates.stream()
                        .filter(other -> !other.getUser().getUserId().equals(me.getUser().getUserId()))
                        .filter(other -> calculateSimilarity(me, other) >= 0.5) // ✅ 유사도 기준 적용
                        .sorted((a, b) -> Double.compare(
                                calculateSimilarity(b, me),
                                calculateSimilarity(a, me)
                        ))
                        .limit(required)
                        .toList();

                if (potentialPartners.size() == required) {
                    List<MatchQueueEntry> matchedGroup = new ArrayList<>(potentialPartners);
                    matchedGroup.add(me);

                    // 유효성 검사
                    validateUsersBeforeMatch(
                            matchedGroup.stream().map(e -> e.getUser().getUserId()).toList(), room);

                    // 게임 생성
                    Game game = new Game();
                    game.setDate(LocalDate.now());
                    game.setTime(LocalTime.now());
                    game.setLocation(room.getLocation());
                    Game savedGame = gameRepository.save(game);

                    // 참가자 저장부분 수정
                    List<GameParticipant> participants = matchedGroup.stream().map(queueEntry -> {
                        GameParticipant p = new GameParticipant();
                        p.setGame(savedGame);
                        p.setUser(queueEntry.getUser());
                        return p;
                    }).toList();
                    gameParticipantRepository.saveAll(participants);

                    // 게임 히스토리 생성
                    GameHistory history = new GameHistory();
                    history.setGame(savedGame);
                    history.setScoreTeamA(0); // 초기값
                    history.setScoreTeamB(0);
                    history.setCompleted(false);
                    gameHistoryRepository.save(history);

                   // 큐 상태 업데이트
                    matchedGroup.forEach(q -> {
                        q.setMatched(true);
                        q.setIsPrematched(true);
                    });
                    matchQueueRepository.saveAll(matchedGroup);

                    // 코드 수정
                    List<NormalUser> users = matchedGroup.stream().map(MatchQueueEntry::getUser).toList();
                    users.forEach(user -> user.setCurrentGame(savedGame));
                    normalUserRepository.saveAll(users);

                    return savedGame;
                }
            }
        }
        return null;
    }

    // ========================== 현장매칭 (구장 기준) ==========================
    public Game matchLiveCourtFromRoom(Long roomId) {
        GameRoom room = gameRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("게임방을 찾을 수 없습니다."));

        List<MatchQueueEntry> queue = matchQueueRepository.findByMatchedFalseAndGameRoom_GameRoomId(roomId);

        // 유효성 검사 추가
        List<Long> userIds = queue.stream()
                .map(e -> e.getUser().getUserId())
                .distinct()
                .toList();
        validateUsersBeforeMatch(userIds, room);

        for (MatchQueueEntry me : queue) {
            List<MatchQueueEntry> candidates = queue.stream()
                    .filter(other -> !other.equals(me))
                    .filter(other -> calculateSimilarity(me, other) >= 0.5) // 유사도 조건 반영
                    .sorted((a, b) -> Double.compare(
                            calculateSimilarity(b, me),
                            calculateSimilarity(a, me)
                    ))
                    .limit(getRequiredMatchCount(me.getProfile().getGameType()))
                    .toList();

            if (candidates.size() == getRequiredMatchCount(me.getProfile().getGameType())) {
                List<MatchQueueEntry> matchedGroup = new ArrayList<>(candidates);
                matchedGroup.add(me);

                // 게임 생성
                Game game = buildGameFromQueueEntries(matchedGroup);
                game.setLocation(room.getLocation()); // 게임방의 장소 정보로 설정
                Game savedGame = gameRepository.save(game);

                // 참가자 저장 수정
                List<GameParticipant> participants = matchedGroup.stream().map(entry -> {
                    GameParticipant p = new GameParticipant();
                    p.setGame(savedGame);
                    p.setUser(entry.getUser());
                    return p;
                }).toList();
                gameParticipantRepository.saveAll(participants);

                // 게임 히스토리 생성
                GameHistory history = new GameHistory();
                history.setGame(savedGame);
                history.setScoreTeamA(0); // 초기값
                history.setScoreTeamB(0);
                history.setCompleted(false);
                gameHistoryRepository.save(history);

                // 큐 상태 업데이트
                matchedGroup.forEach(entry -> {
                    entry.setMatched(true);
                    entry.setIsPrematched(false);
                });
                matchQueueRepository.saveAll(matchedGroup);

                // 사용자 게임 연결 (타입 수정)
                List<NormalUser> users = matchedGroup.stream().map(MatchQueueEntry::getUser).toList();
                users.forEach(user -> user.setCurrentGame(savedGame));
                normalUserRepository.saveAll(users);


                return savedGame;
            }
        }
        return null;
    }

    // ========================== 사전매칭 (동네 기준) ==========================
    public GameRoom createPreLocationMeetingRoomFromUser(Long userId, LocalDate date, LocalTime time, Location location) {
        MatchQueueEntry me = matchQueueRepository.findByUser_UserIdAndMatchedFalse(userId)
                .stream()
                .filter(entry -> entry.getGameRoom() == null)
                .filter(entry -> entry.getDate().equals(date))
                .filter(entry -> entry.getTime().equals(time))
                .filter(entry -> entry.getMatchType() == MatchQueueType.QUEUE_PRE)
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사전매칭 큐에 등록된 유저가 없습니다."));

        List<MatchQueueEntry> queue = matchQueueRepository.findByMatchedFalseAndMatchTypeAndGameRoomIsNull(MatchQueueType.QUEUE_PRE)
                .stream()
                .filter(other -> !other.getUser().getUserId().equals(userId))
                .filter(other -> other.getDate().equals(date))
                .filter(other -> other.getTime().equals(time))
                .toList();

        List<MatchQueueEntry> candidates = queue.stream()
                .filter(other -> isNearby(me.getLocation(), other.getLocation()))
                .filter(other -> isSimilarEnough(me, other))
                .sorted((a, b) -> Double.compare(calculateSimilarity(b, me), calculateSimilarity(a, me)))
                .limit(getRequiredMatchCount(me.getProfile().getGameType()))
                .toList();

        if (candidates.size() == getRequiredMatchCount(me.getProfile().getGameType())) {
            List<MatchQueueEntry> matchedGroup = new ArrayList<>(candidates);
            matchedGroup.add(me);

            List<Long> userIds = matchedGroup.stream().map(e -> e.getUser().getUserId()).toList();
            validateUsersBeforeMatch(userIds, null);

            matchedGroup.forEach(e -> e.setMatched(true));
            matchQueueRepository.saveAll(matchedGroup);

            List<NormalUser> users = matchedGroup.stream().map(MatchQueueEntry::getUser).toList();

            GameRoom room = new GameRoom();
            room.setDate(date);
            room.setTime(time);
            room.setLocation(me.getLocation());
            room.setParticipants(users);

            GameRoom savedRoom = gameRoomRepository.save(room);
            users.forEach(u -> u.setGameRoom(savedRoom));
            normalUserRepository.saveAll(users);

            return savedRoom;
        }

        return null;
    }

    // ========================== 게임 생성 ==========================
    public Game buildGameFromQueueEntries(List<MatchQueueEntry> entries) {
        Game game = new Game();
        MatchQueueEntry base = entries.get(0);
        game.setDate(base.getDate());
        game.setTime(base.getTime());
        game.setLocation(base.getLocation());

        Game savedGame = gameRepository.save(game);

        // GameParticipant 수정 부분
        List<GameParticipant> participants = entries.stream().map(entry -> {
            GameParticipant participant = new GameParticipant();
            participant.setGame(savedGame);
            participant.setUser(entry.getUser());
            return participant;
        }).collect(Collectors.toList());
        gameParticipantRepository.saveAll(participants);

        // NormalUser 객체에 currentGame 설정
        for (GameParticipant gp : participants) {
            NormalUser user = gp.getUser();
            user.setCurrentGame(savedGame);
            normalUserRepository.save(user);
        }

        return savedGame;
    }

    // ========================== 유사도 계산 ==========================
    private boolean isNearby(Location a, Location b) {
        return GeoUtil.calculateDistance(a.getLatitude(), a.getLongitude(),
                                         b.getLatitude(), b.getLongitude()) <= 300;
    }

    private boolean isSimilarEnough(MatchQueueEntry a, MatchQueueEntry b) {
        return calculateSimilarity(a, b) >= 0.5;
    }

    private double calculateSimilarity(MatchQueueEntry a, MatchQueueEntry b) {
        int match = 0;
        if (a.getProfile().getAgeGroup().equals(b.getProfile().getAgeGroup())) match++;
        if (a.getProfile().getGameType().equals(b.getProfile().getGameType())) match++;
        if (a.getProfile().getPlayStyle().equals(b.getProfile().getPlayStyle())) match++;
        if (Math.abs(a.getMmr().getRating() - b.getMmr().getRating()) <= a.getMmr().getTolerance()) match++;

        return match / 4.0;
    }

    private int getRequiredMatchCount(String gameType) {
        return switch (gameType) {
            case "단식" -> 1;
            case "혼합복식", "남자복식", "여자복식" -> 3;
            default -> throw new IllegalArgumentException("지원하지 않는 게임 타입: " + gameType);
        };
    }
}
