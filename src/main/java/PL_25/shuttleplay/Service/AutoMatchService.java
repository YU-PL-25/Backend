package PL_25.shuttleplay.Service;


import PL_25.shuttleplay.Entity.Game.*;
import PL_25.shuttleplay.Entity.Location;
import PL_25.shuttleplay.Entity.User.MMR;
import PL_25.shuttleplay.Entity.User.NormalUser;
import PL_25.shuttleplay.Entity.User.Profile;
import PL_25.shuttleplay.Repository.*;
import PL_25.shuttleplay.Util.GeoUtil;
import PL_25.shuttleplay.dto.Matching.AutoMatchRequest;
import PL_25.shuttleplay.dto.Matching.GameDTO;
import PL_25.shuttleplay.dto.Matching.GameRoomDTO;
import PL_25.shuttleplay.dto.Matching.PlayerDTO;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
    private final GameParticipantRepository gameParticipantRepository;

    @PersistenceContext
    private EntityManager entityManager;

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

    // 사전 매칭 큐 등록용 (게임방 매칭)
    public MatchQueueResponse registerToQueue(Long userId, AutoMatchRequest request) {
        return registerToQueue(userId, null, request, true); // 내부 통합 메서드로 위임 (isPreMatch = true)
    }

    // 현장 매칭 큐 등록용 (게임 매칭)
    public MatchQueueResponse registerToQueue(Long userId, Long gameRoomId, AutoMatchRequest request) {
        return registerToQueue(userId, gameRoomId, request, false); // 내부 통합 메서드로 위임 (isPreMatch = false)
    }

    // 매칭 큐 등록 내부 통합 처리 메서드
    // - gameRoomId는 현장 게임 매칭 시에만 사용됨
    // - isPreMatch가 true면 사전 매칭, false면 현장 매칭
    private MatchQueueResponse registerToQueue(Long userId, Long gameRoomId, AutoMatchRequest request, boolean isPreMatch) {
        NormalUser user = normalUserRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없음"));

        // 사전 매칭인 경우 location 정보 필수
        if (isPreMatch) {
            if (request.getLocation() == null ||
                    request.getLocation().getCourtName() == null ||
                    request.getLocation().getCourtAddress() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "사전 매칭 시 코트 이름과 주소는 필수입니다.");
            }
        }

        // 이미 매칭 큐에 등록된 유저인 경우 중복 방지
        boolean alreadyQueued =
                matchQueueRepository.existsByUser_UserIdAndMatchedFalseAndGameRoomIsNull(userId) ||
                        matchQueueRepository.existsByUser_UserIdAndMatchedFalseAndGameRoomIsNotNull(userId);

        if (alreadyQueued) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 매칭 큐에 등록된 상태입니다.");
        }

        // MatchQueueEntry 생성 및 공통 속성 세팅
        MatchQueueEntry entry = new MatchQueueEntry();
        entry.setUser(user);

        // db에 저장된 사용자 profile, mmr 가져와서 매칭큐 엔트리 등록 진행
        Profile profile = user.getProfile();
        if (profile == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "DB에 프로필 정보가 없습니다.");
        }
        entry.setProfile(profile);

        MMR mmr = user.getMmr();
        if (mmr == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "DB에 MMR 정보가 없습니다.");
        }
        entry.setMmr(mmr);
        entry.setIsPrematched(isPreMatch);
        entry.setMatched(false);
        entry.setRequiredMatchCount(request.getRequiredMatchCount());

        if (isPreMatch) {
            // 사전 매칭용 설정
            entry.setMatchType(MatchQueueType.QUEUE_PRE);
            entry.setLocation(request.getLocation());
            entry.setDate(request.getDate());
            entry.setTime(request.getTime());
            entry.setGameRoom(null); // 방 없음
        } else {
            // 현장 매칭용 설정
            if (gameRoomId == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "현장 매칭 시 gameRoomId는 필수입니다.");
            }

            GameRoom room = gameRoomRepository.findById(gameRoomId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "해당 게임방을 찾을 수 없습니다."));

            entry.setMatchType(MatchQueueType.QUEUE_LIVE_AUTO);
            entry.setGameRoom(room);
            entry.setLocation(room.getLocation()); // 위치는 방 기준
            entry.setDate(LocalDate.now());
            entry.setTime(LocalTime.now());
        }

        // 매칭 큐 저장 및 응답
        MatchQueueEntry saved = matchQueueRepository.save(entry);
        return new MatchQueueResponse(saved);
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
    @Transactional
    public Game matchLiveCourtFromRoom(Long roomId) {
        GameRoom room = gameRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("게임방을 찾을 수 없습니다."));

        // 게임방에 등록된 유저 중 아직 매칭되지 않은 QUEUE_LIVE 큐 가져오기
        List<MatchQueueEntry> queue = matchQueueRepository
                .findByMatchedFalseAndGameRoom_GameRoomId(roomId).stream()
                .filter(entry -> entry.getMatchType() == MatchQueueType.QUEUE_LIVE_AUTO)
                .toList();

        List<Long> userIds = queue.stream()
                .map(e -> e.getUser().getUserId())
                .distinct()
                .toList();
        validateUsersBeforeMatch(userIds, room);

        for (MatchQueueEntry me : queue) {
            // 사용자 요청에 따른 인원 수 반영
            int required = me.getRequiredMatchCount() > 0
                    ? me.getRequiredMatchCount() - 1
                    : getRequiredMatchCount(me.getProfile().getGameType());

            List<MatchQueueEntry> candidates = queue.stream()
                    .filter(other -> !other.equals(me))
                    .filter(other -> isSimilarEnough(me, other))
                    .sorted((a, b) -> Double.compare(
                            calculateSimilarity(b, me),
                            calculateSimilarity(a, me)
                    ))
                    .limit(required)
                    .toList();

            if (candidates.size() == required) {
                List<MatchQueueEntry> matchedGroup = new ArrayList<>(candidates);
                matchedGroup.add(me);

                // 유효성 검사
                validateUsersBeforeMatch(matchedGroup.stream().map(e -> e.getUser().getUserId()).toList(), room);

                // 게임 생성 - 게임방의 정보 기준으로 설정
                Game game = new Game();
                game.setDate(room.getDate());
                game.setTime(room.getTime());
                game.setLocation(room.getLocation());game.setGameRoom(room);
                game.setMatchType(String.valueOf(MatchQueueType.QUEUE_LIVE_AUTO));
                game.setStatus(GameStatus.WAITING);

                // 참가자 리스트 구성 및 게임과 양방향 연결
                List<GameParticipant> participants = new ArrayList<>();
                for (MatchQueueEntry entry : matchedGroup) {
                    GameParticipantId id = new GameParticipantId(null, entry.getUser().getUserId());

                    // GameParticipant 생성 시 game 참조 설정
                    GameParticipant participant = new GameParticipant(entry.getUser(), game);
                    participants.add(participant);
                }

                game.setParticipants(participants); // cascade로 저장되게 연결

                Game savedGame = gameRepository.save(game); // Game과 참가자 함께 저장됨

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
    // dto 써서 응답
    @Transactional
    public GameDTO matchLiveCourtFromRoomDto(Long roomId) {
        Game game = matchLiveCourtFromRoom(roomId); // 기존 로직 그대로 재사용

        if (game == null) return null;

        // GameParticipant → PlayerDTO 변환
        List<PlayerDTO> players = game.getParticipants().stream()
                .map(participant -> {
                    NormalUser user = participant.getUser();
                    return PlayerDTO.builder()
                            .userId(user.getUserId())
                            .nickname(user.getNickname())
                            .rank(String.valueOf(user.getRank()))
                            .build();
                })
                .toList();

        // GameDTO 변환
        return GameDTO.builder()
                .gameId(game.getGameId())
                .matchType(game.getMatchType())
                .status(game.getStatus().name())
                .date(game.getDate().toString())
                .time(game.getTime().toString())
                .players(players)
                .build();
    }


    // 사전 매칭 (동네 기준) - 큐에 등록된 정보만으로 자동 매칭 및 게임방 생성
    public GameRoom createPreLocationMeetingRoomFromUser(Long userId) {
        // 1. 해당 유저의 매칭 큐 엔트리 조회
        MatchQueueEntry me = matchQueueRepository.findByUser_UserIdAndMatchedFalse(userId)
                .stream()
                .filter(entry -> entry.getGameRoom() == null)
                .filter(entry -> entry.getMatchType() == MatchQueueType.QUEUE_PRE)
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사전매칭 큐에 등록된 유저가 없습니다."));

        // 2. 등록된 정보에서 date, time, location 가져오기
        LocalDate date = me.getDate();
        LocalTime time = me.getTime();
        Location location = me.getLocation();

        // 3. 같은 조건 + 300m 이내 유저들 조회
        List<MatchQueueEntry> queue = matchQueueRepository.findByMatchedFalseAndMatchTypeAndGameRoomIsNull(MatchQueueType.QUEUE_PRE)
                .stream()
                .filter(other -> !other.getUser().getUserId().equals(userId))
                .filter(other -> other.getDate().equals(date))
                .filter(other -> other.getTime().equals(time))
                .filter(other -> isNearby(location, other.getLocation())) // 거리 기준
                .toList();

        // 4. 게임 인원 충족 여부 확인
        int required = me.getRequiredMatchCount() > 0
                ? me.getRequiredMatchCount() - 1
                : getRequiredMatchCount(me.getProfile().getGameType());

        List<MatchQueueEntry> candidates = queue.stream().limit(required).toList();

        if (candidates.size() == required) {
            // 5. 최종 매칭 대상 구성
            List<MatchQueueEntry> matchedGroup = new ArrayList<>(candidates);
            matchedGroup.add(me);

            // 6. 유저 상태 확인
            List<Long> userIds = matchedGroup.stream().map(e -> e.getUser().getUserId()).toList();
            validateUsersBeforeMatch(userIds, null);

            // 7. 큐 상태 업데이트
            matchedGroup.forEach(e -> e.setMatched(true));
            matchQueueRepository.saveAll(matchedGroup);

            // 8. 게임방 생성
            List<NormalUser> users = matchedGroup.stream().map(MatchQueueEntry::getUser).toList();

            GameRoom room = new GameRoom();
            room.setDate(date);
            room.setTime(time);
            room.setLocation(location);
            room.setParticipants(users);

            GameRoom savedRoom = gameRoomRepository.save(room);

            // 9. 유저에 게임방 연결
            users.forEach(u -> u.setGameRoom(savedRoom));
            normalUserRepository.saveAll(users);

            return savedRoom;
        }

        return null;
    }

    @Transactional
    public GameRoomDTO createPreLocationMeetingRoomDto(Long userId) {
        GameRoom room = createPreLocationMeetingRoomFromUser(userId);
        if (room == null) return null;

        return GameRoomDTO.builder()
                .gameRoomId(room.getGameRoomId())
                .managerId(room.getCreatedBy() != null ? room.getCreatedBy().getUserId() : null)
                .title(room.getTitle())
                .locationName(room.getLocation().getCourtName())
                .locationAddress(room.getLocation().getCourtAddress())
                .games(room.getGameList().stream().map(this::convertToGameDto).toList())
                .build();
    }


    // 사전 매칭 (구장 기준) - 큐에 등록된 정보만으로 게임방 자동 생성
    public GameRoom createPreGymMeetingRoomFromUser(Long userId) {
        // 1. userId로 큐에서 사전 매칭 등록 정보 찾기
        MatchQueueEntry me = matchQueueRepository.findByUser_UserIdAndMatchedFalse(userId)
                .stream()
                .filter(entry -> entry.getGameRoom() == null)
                .filter(entry -> entry.getMatchType() == MatchQueueType.QUEUE_PRE)
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사전매칭 큐에 등록된 유저가 없습니다."));

        // 2. 등록된 큐 정보에서 날짜/시간/위치 가져오기
        LocalDate date = me.getDate();
        LocalTime time = me.getTime();
        Location location = me.getLocation();

        // 3. 동일 조건(날짜/시간/위치) 유저 큐 조회
        List<MatchQueueEntry> queue = matchQueueRepository.findByMatchedFalseAndMatchTypeAndGameRoomIsNull(MatchQueueType.QUEUE_PRE)
                .stream()
                .filter(other -> !other.getUser().getUserId().equals(userId))
                .filter(other -> other.getDate().equals(date))
                .filter(other -> other.getTime().equals(time))
                .filter(other -> isSameCourt(location, other.getLocation()))
                .toList();

        // 4. 인원 수 충족 여부 확인
        int required = me.getRequiredMatchCount() > 0
                ? me.getRequiredMatchCount() - 1
                : getRequiredMatchCount(me.getProfile().getGameType());

        List<MatchQueueEntry> candidates = queue.stream().limit(required).toList();

        if (candidates.size() == required) {
            // 5. 매칭 대상 구성
            List<MatchQueueEntry> matchedGroup = new ArrayList<>(candidates);
            matchedGroup.add(me);

            // 6. 유저 상태 검증
            List<Long> userIds = matchedGroup.stream().map(e -> e.getUser().getUserId()).toList();
            validateUsersBeforeMatch(userIds, null);

            // 7. 매칭 완료 처리
            matchedGroup.forEach(e -> e.setMatched(true));
            matchQueueRepository.saveAll(matchedGroup);

            // 8. 게임방 생성
            List<NormalUser> users = matchedGroup.stream().map(MatchQueueEntry::getUser).toList();

            GameRoom room = new GameRoom();
            room.setDate(date);
            room.setTime(time);
            room.setLocation(location);
            room.setParticipants(users);

            GameRoom savedRoom = gameRoomRepository.save(room);

            // 9. 유저에게 게임방 지정
            users.forEach(u -> u.setGameRoom(savedRoom));
            normalUserRepository.saveAll(users);

            return savedRoom;
        }

        // 매칭 실패 시 null 반환
        return null;
    }

    @Transactional
    public GameRoomDTO createPreGymMeetingRoomDto(Long userId) {
        GameRoom room = createPreGymMeetingRoomFromUser(userId);
        if (room == null) return null;

        return GameRoomDTO.builder()
                .gameRoomId(room.getGameRoomId())
                .managerId(room.getCreatedBy() != null ? room.getCreatedBy().getUserId() : null)
                .title(room.getTitle())
                .locationName(room.getLocation().getCourtName())
                .locationAddress(room.getLocation().getCourtAddress())
                .games(room.getGameList().stream().map(this::convertToGameDto).toList())
                .build();
    }

    private GameDTO convertToGameDto(Game game) {
        List<PlayerDTO> players = game.getParticipants().stream()
                .map(p -> {
                    NormalUser u = p.getUser();
                    return PlayerDTO.builder()
                            .userId(u.getUserId())
                            .nickname(u.getNickname())
                            .rank(String.valueOf(u.getRank()))
                            .build();
                }).toList();

        return GameDTO.builder()
                .gameId(game.getGameId())
                .matchType(game.getMatchType())
                .status(game.getStatus().name())
                .date(game.getDate().toString())
                .time(game.getTime().toString())
                .players(players)
                .build();
    }

    // 구장 위치가 같은지 확인 (이름 + 주소 + 좌표는 허용 오차 내일 때)
    private boolean isSameCourt(Location a, Location b) {
        final double TOLERANCE = 0.0003; // 약 30m 이내

        return a.getCourtName().equals(b.getCourtName())
                && a.getCourtAddress().equals(b.getCourtAddress())
                && Math.abs(a.getLatitude() - b.getLatitude()) <= TOLERANCE
                && Math.abs(a.getLongitude() - b.getLongitude()) <= TOLERANCE;
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