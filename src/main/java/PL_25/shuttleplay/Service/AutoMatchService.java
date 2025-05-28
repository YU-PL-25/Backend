package PL_25.shuttleplay.Service;


import PL_25.shuttleplay.Entity.Game.*;
import PL_25.shuttleplay.Entity.Location;
import PL_25.shuttleplay.Entity.User.MMR;
import PL_25.shuttleplay.Entity.User.NormalUser;
import PL_25.shuttleplay.Entity.User.Profile;
import PL_25.shuttleplay.Repository.*;
import PL_25.shuttleplay.Util.GeoUtil;
import PL_25.shuttleplay.dto.Matching.AutoMatchRequest;
import PL_25.shuttleplay.dto.Matching.MMRDTO;
import PL_25.shuttleplay.dto.Matching.ProfileDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

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

        // location 필드 입력되었는지 검사
        if (request.getLocation() == null ||
                request.getLocation().getCourtName() == null ||
                request.getLocation().getCourtAddress() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "코트 이름과 주소는 필수입니다.");
        }

        // 해당 유저가 이미 room에 입장되어있는지 검사
        Long roomId = user.getGameRoom() != null ? user.getGameRoom().getGameRoomId() : null;

        if (roomId != null &&
                matchQueueRepository.existsByUser_UserIdAndGameRoom_GameRoomIdAndMatchedFalse(userId, roomId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 해당 게임방에 매칭 등록이 되어있습니다.");
        }

        if (matchQueueRepository.existsByUser_UserIdAndMatchedFalseAndGameRoomIsNotNull(userId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 다른 게임방에 매칭 등록이 되어있습니다.");
        }

        // 대기열 엔트리 객체 생성
        MatchQueueEntry entry = new MatchQueueEntry();
        entry.setUser(user);

        // 해당 유저의 profile 정보 가져오기
        Profile profile = user.getProfile(); // DB에서 가져오기
        if (profile == null) {
            // DB에 없으면 dto 요청에서 가져옴
            ProfileDTO dto = request.getProfile();
            if (dto == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "프로필 정보가 없습니다. DB에도 없고 요청에도 없습니다.");
            }

            profile = new Profile();
            profile.setGameType(dto.getGameType());
            profile.setAgeGroup(dto.getAgeGroup());
            profile.setPlayStyle(dto.getPlayStyle());
        }
        entry.setProfile(profile); // 큐 엔트리에 등록

        // 해당 유저의 MMR 정보 가져오기 (DB에 있으면 가져오고, 없으면 request dto로 입력받은거 사용)
        MMR mmr = user.getMmr();
        if (mmr == null) {
            MMRDTO mmrDto = request.getMmr();
            if (mmrDto == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "MMR 정보가 없습니다. DB에도 없고 요청에도 없습니다.");
            }

            mmr = new MMR();
            mmr.setRating(mmrDto.getRating());
            mmr.setTolerance(mmrDto.getTolerance());
        }
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
            // 현장매칭 큐 등록 (날짜/시간/거리 고려하지 않음)
            entry.setMatchType(MatchQueueType.QUEUE_LIVE);
            entry.setDate(null); // 날짜 무시
            entry.setTime(null); // 시간 무시

            // 반드시 GameRoom이 있어야 함
            if (user.getGameRoom() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "현장 매칭을 위해 게임방이 필요합니다.");
            }

            entry.setGameRoom(user.getGameRoom());
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

    // 구장 기준 자동 매칭(현장/사전 전부 해당, 날짜/시간/위치 고려 없이 게임방 기준으로만 매칭)
    public Game matchLiveCourtFromRoom(Long roomId) {
        GameRoom room = gameRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("게임방을 찾을 수 없습니다."));

        // 게임방에 등록된 유저 중 아직 매칭되지 않은 QUEUE_LIVE 큐 가져오기
        List<MatchQueueEntry> queue = matchQueueRepository
                .findByMatchedFalseAndGameRoom_GameRoomId(roomId).stream()
                .filter(entry -> entry.getMatchType() == MatchQueueType.QUEUE_LIVE)
                .toList();

        List<Long> userIds = queue.stream()
                .map(e -> e.getUser().getUserId())
                .distinct()
                .toList();
        validateUsersBeforeMatch(userIds, room);

        for (MatchQueueEntry me : queue) {
            List<MatchQueueEntry> candidates = queue.stream()
                    .filter(other -> !other.equals(me))
                    .filter(other -> isSimilarEnough(me, other))
                    .sorted((a, b) -> Double.compare(
                            calculateSimilarity(b, me),
                            calculateSimilarity(a, me)
                    ))
                    .limit(getRequiredMatchCount(me.getProfile().getGameType()))
                    .toList();

            if (candidates.size() == getRequiredMatchCount(me.getProfile().getGameType())) {
                List<MatchQueueEntry> matchedGroup = new ArrayList<>(candidates);
                matchedGroup.add(me);

                // 유효성 검사
                validateUsersBeforeMatch(matchedGroup.stream().map(e -> e.getUser().getUserId()).toList(), room);

                // 게임 생성 - 게임방의 정보 기준으로 설정
                Game game = new Game();
                game.setDate(room.getDate());
                game.setTime(room.getTime());
                game.setLocation(room.getLocation());
                Game savedGame = gameRepository.save(game);

                // 참가자 저장
                List<GameParticipant> participants = matchedGroup.stream().map(entry -> {
                    GameParticipant p = new GameParticipant();
                    p.setGame(savedGame);
                    p.setUser(entry.getUser());
                    return p;
                }).toList();
                gameParticipantRepository.saveAll(participants);

                // 히스토리 생성
                GameHistory history = new GameHistory();
                history.setGame(savedGame);
                history.setScoreTeamA(0);
                history.setScoreTeamB(0);
                history.setCompleted(false);
                gameHistoryRepository.save(history);

                // 큐 상태 업데이트
                matchedGroup.forEach(entry -> {
                    entry.setMatched(true);
                    entry.setIsPrematched(false);
                });
                matchQueueRepository.saveAll(matchedGroup);

                // 유저 현재 게임 설정
                List<NormalUser> users = matchedGroup.stream().map(MatchQueueEntry::getUser).toList();
                users.forEach(user -> user.setCurrentGame(savedGame));
                normalUserRepository.saveAll(users);

                return savedGame;
            }
        }
        return null;
    }

    // 사전매칭 (동네 기준)
    public GameRoom createPreLocationMeetingRoomFromUser(Long userId, LocalDate date, LocalTime time, Location location) {
        // 1. 해당 user의 큐 엔트리 가져오기
        MatchQueueEntry me = matchQueueRepository.findByUser_UserIdAndMatchedFalse(userId)
                .stream()
                .filter(entry -> entry.getGameRoom() == null)
                .filter(entry -> entry.getDate().equals(date))
                .filter(entry -> entry.getTime().equals(time))
                .filter(entry -> entry.getMatchType() == MatchQueueType.QUEUE_PRE)
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사전매칭 큐에 등록된 유저가 없습니다."));

        // 2. 나머지 큐에서 매칭 조건(날짜, 시간, 거리)만 필터링
        List<MatchQueueEntry> queue = matchQueueRepository.findByMatchedFalseAndMatchTypeAndGameRoomIsNull(MatchQueueType.QUEUE_PRE)
                .stream()
                .filter(other -> !other.getUser().getUserId().equals(userId))
                .filter(other -> other.getDate().equals(date))
                .filter(other -> other.getTime().equals(time))
                .filter(other -> isNearby(location, other.getLocation()))
                .toList();

        // 3. 게임 인원수 기준에 맞게 매칭 시도
        int required = getRequiredMatchCount(me.getProfile().getGameType());
        List<MatchQueueEntry> candidates = queue.stream()
                .limit(required)
                .toList();

        if (candidates.size() == required) {
            List<MatchQueueEntry> matchedGroup = new ArrayList<>(candidates);
            matchedGroup.add(me); // 본인 추가

            // 유효성 검사
            List<Long> userIds = matchedGroup.stream().map(e -> e.getUser().getUserId()).toList();
            validateUsersBeforeMatch(userIds, null);

            // 매칭 상태 변경
            matchedGroup.forEach(e -> e.setMatched(true));
            matchQueueRepository.saveAll(matchedGroup);

            // 게임방 생성
            List<NormalUser> users = matchedGroup.stream().map(MatchQueueEntry::getUser).toList();

            GameRoom room = new GameRoom();
            room.setDate(date);
            room.setTime(time);
            room.setLocation(location);
            room.setParticipants(users);

            GameRoom savedRoom = gameRoomRepository.save(room);

            // 유저에 게임방 할당
            users.forEach(u -> u.setGameRoom(savedRoom));
            normalUserRepository.saveAll(users);

            return savedRoom;
        }

        return null;
    }

    // 300m 거리 이내 인지 확인
    private boolean isNearby(Location a, Location b) {
        return GeoUtil.calculateDistance(a.getLatitude(), a.getLongitude(),
                b.getLatitude(), b.getLongitude()) <= 300;
    }
    // 유사도 계산
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

    // 단복식 구분해 매칭시 필요한 사용자 계산
    private int getRequiredMatchCount(String gameType) {
        return switch (gameType) {
            case "단식" -> 1;
            case "혼합복식", "남자복식", "여자복식" -> 3;
            default -> throw new IllegalArgumentException("지원하지 않는 게임 타입: " + gameType);
        };
    }
}