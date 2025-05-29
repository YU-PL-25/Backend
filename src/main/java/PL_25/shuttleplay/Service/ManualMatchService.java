package PL_25.shuttleplay.Service;

import PL_25.shuttleplay.Entity.Game.*;
import PL_25.shuttleplay.Entity.Location;
import PL_25.shuttleplay.Entity.User.NormalUser;
import PL_25.shuttleplay.Repository.*;
import PL_25.shuttleplay.Util.GeoUtil;
import PL_25.shuttleplay.dto.Matching.ManualMatchRequest;
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
public class ManualMatchService {

    private final GameRoomRepository gameRoomRepository;
    private final GameRepository gameRepository;
    private final MatchQueueRepository matchQueueRepository;
    private final NormalUserRepository normalUserRepository;
    private final GameParticipantRepository gameParticipantRepository;

    @PersistenceContext
    private EntityManager entityManager;

   // 매칭 전 유저 상태 확인 (게임중이거나, 다른 방에 대기중이거나)
     public void validateUsersBeforeMatch(List<Long> userIds, GameRoom currentRoomOrNull) {
        for (Long userId : userIds) {
            boolean inGame = gameParticipantRepository.existsByUser_UserIdAndGame_Status(userId, GameStatus.ONGOING);
            if (inGame) {
                throw new IllegalStateException("userId=" + userId + " 는 이미 게임 중입니다.");
            }
            List<MatchQueueEntry> existing = matchQueueRepository.findByUser_UserIdAndMatchedFalse(userId);
            for (MatchQueueEntry entry : existing) {
                if (entry.getGameRoom() != null &&
                        (currentRoomOrNull == null || !entry.getGameRoom().getGameRoomId().equals(currentRoomOrNull.getGameRoomId()))) {
                    throw new IllegalStateException("userId=" + userId + " 는 다른 방에 대기 중입니다.");
                }
            }
        }
    }

    // 사전 매칭 큐 등록용(게임방)
    public MatchQueueResponse registerToQueue(Long userId, ManualMatchRequest request) {
        return registerToQueue(userId, null, request, true); // 내부 통합 메서드로 위임
    }

    // 현장 매칭 큐 등록용(게임)
    public MatchQueueResponse registerToQueue(Long userId, Long gameRoomId, ManualMatchRequest request) {
        return registerToQueue(userId, gameRoomId, request, false); // 내부 통합 메서드로 위임
    }

    // 매칭 큐 등록 내부 통합 처리 메서드 (gameRoomId는 현장 매칭일 때만 사용됨)
    private MatchQueueResponse registerToQueue(Long userId, Long gameRoomId, ManualMatchRequest request, boolean isPreMatch) {
        NormalUser user = normalUserRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없음"));

        // 사전 매칭인 경우 location 필수
        if (isPreMatch) {
            if (request.getLocation() == null ||
                    request.getLocation().getCourtName() == null ||
                    request.getLocation().getCourtAddress() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "사전 매칭 시 코트 이름과 주소는 필수입니다.");
            }
        }

        // 이미 매칭 큐에 등록된 경우 방어
        boolean alreadyQueued =
                matchQueueRepository.existsByUser_UserIdAndMatchedFalseAndGameRoomIsNull(userId) ||
                        matchQueueRepository.existsByUser_UserIdAndMatchedFalseAndGameRoomIsNotNull(userId);

        if (alreadyQueued) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 매칭 큐에 등록된 상태입니다.");
        }

        MatchQueueEntry entry = new MatchQueueEntry();
        entry.setUser(user);
        entry.setProfile(user.getProfile());
        entry.setMmr(user.getMmr());
        entry.setIsPrematched(isPreMatch);
        entry.setMatched(false);

        if (isPreMatch) {
            // 사전 매칭용 세팅
            entry.setMatchType(MatchQueueType.QUEUE_PRE);
            entry.setLocation(request.getLocation());
            entry.setDate(request.getDate());
            entry.setTime(request.getTime());
            entry.setGameRoom(null);
        } else {
            // 현장 매칭용 세팅
            if (gameRoomId == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "현장 매칭 시 gameRoomId는 필수입니다.");
            }

            GameRoom room = gameRoomRepository.findById(gameRoomId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "해당 게임방을 찾을 수 없습니다."));

            entry.setMatchType(MatchQueueType.QUEUE_LIVE);
            entry.setGameRoom(room);
            entry.setLocation(room.getLocation()); // 위치는 게임방 기준
            entry.setDate(LocalDate.now());
            entry.setTime(LocalTime.now());
        }

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

    // 게임방 찾기
    public GameRoom getGameRoomById(Long roomId) {
        return gameRoomRepository.findById(roomId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "게임방을 찾을 수 없습니다."));
    }

    // 수동 매칭을 하기 위해 요청한 사용자 ID가 방장인지 확인
    private void validateRoomCreator(Long requesterId, GameRoom room) {
        if (!room.getCreatedBy().getUserId().equals(requesterId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "방 생성자만 수동 매칭을 실행할 수 있습니다.");
        }
    }

    // 구장 수동 매칭(사전/현장 동일)
    @Transactional
    public Game createLiveGameFromRoom(GameRoom room, List<Long> userIds, Long requesterId) {
        // 요청자가 방 생성자인지 검증
        validateRoomCreator(requesterId, room);

        // 유저 상태 검사 (게임 중이거나 다른 방 대기 중인지)
        validateUsersBeforeMatch(userIds, room);

        // 방장은 큐 검사에서 제외
        List<Long> filteredUserIds = userIds.stream()
                .filter(id -> !id.equals(requesterId))
                .toList();

        // 매칭 큐에서 실제로 대기 중인 유저들 필터링
        List<MatchQueueEntry> entries = matchQueueRepository
                .findByUser_UserIdInAndMatchedFalse(filteredUserIds)
                .stream()
                .filter(e -> e.getMatchType() == MatchQueueType.QUEUE_LIVE)
                .toList();

        // 큐에 없는 사람이 있다면 예외 발생
        if (entries.size() != filteredUserIds.size()) {
            throw new IllegalArgumentException("일부 사용자가 매칭 큐에 없거나 이미 매칭되었습니다.");
        }

        // 매칭 상태 업데이트
        entries.forEach(e -> {
            e.setMatched(true);
            e.setIsPrematched(false);
        });
        matchQueueRepository.saveAll(entries);

        // 새 게임 생성
        Game game = new Game();
        game.setDate(LocalDate.now());
        game.setTime(LocalTime.now());
        game.setLocation(room.getLocation());

        // 참가자 저장 (중복 여부 검사하여 추가)
        List<GameParticipant> participants = new ArrayList<>();

        for (MatchQueueEntry entry : entries) {
            Long userId = entry.getUser().getUserId();
            GameParticipantId id = new GameParticipantId(null, userId);

            // GameParticipant에 game 직접 연결하여 cascade 적용되도록 설정
            GameParticipant participant = new GameParticipant(entry.getUser(), game);
            participants.add(participant);
        }

        game.setParticipants(participants); // game → participants 관계 설정
        Game savedGame = gameRepository.save(game); // cascade로 GameParticipant까지 저장됨

        // 매칭된 유저들의 currentGame 필드 업데이트
        List<NormalUser> users = entries.stream().map(MatchQueueEntry::getUser).toList();
        users.forEach(user -> user.setCurrentGame(savedGame));
        normalUserRepository.saveAll(users);

        return savedGame;
    }

    // 사전 수동 매칭(동네 기반) - 사용자가 입력한 위치/날짜/시간을 기준으로 큐 등록 + 300m 이내 게임방 조회
    public List<GameRoom> registerQueueAndFindNearbyRooms(Long userId, ManualMatchRequest request) {
        // 매칭 큐 등록
        registerToQueue(userId, request);

        Location loc = request.getLocation();

        return gameRoomRepository.findByDateAndTime(request.getDate(), request.getTime()).stream()
                .filter(room -> {
                    Location roomLoc = room.getLocation();
                    if (roomLoc == null || loc == null) return false;

                    // 거리 계산
                    double distance = GeoUtil.calculateDistance(
                            loc.getLatitude(), loc.getLongitude(),
                            roomLoc.getLatitude(), roomLoc.getLongitude()
                    );

                    return distance <= 300;
                })
                .toList();
    }

    // 사전 수동 매칭(구장 기반) - 사용자가 입력한 위치/날짜/시간을 기준으로 큐 등록 + 동일 구장 게임방 조회
    public List<GameRoom> registerQueueAndFindMatchingRooms(Long userId, ManualMatchRequest request) {
        // 매칭 큐 등록
        registerToQueue(userId, request);

        Location reqLoc = request.getLocation();
        double latThreshold = 0.001;   // 약 ±100m 오차 허용
        double lonThreshold = 0.001;

        return gameRoomRepository.findByDateAndTime(request.getDate(), request.getTime()).stream()
                .filter(room -> {
                    Location roomLoc = room.getLocation();

                    // null-safe 비교
                    if (roomLoc == null || reqLoc == null) return false;
                    if (!safeEquals(roomLoc.getCourtName(), reqLoc.getCourtName())) return false;
                    if (!safeEquals(roomLoc.getCourtAddress(), reqLoc.getCourtAddress())) return false;

                    // 좌표는 일정 오차 범위 내 허용
                    double latDiff = Math.abs(roomLoc.getLatitude() - reqLoc.getLatitude());
                    double lonDiff = Math.abs(roomLoc.getLongitude() - reqLoc.getLongitude());

                    return latDiff <= latThreshold && lonDiff <= lonThreshold;
                })
                .toList();
    }

    // 문자열 null-safe 비교
    private boolean safeEquals(String a, String b) {
        return a != null && b != null && a.equals(b);
    }

    // 사전 수동 매칭(동네, 구장) - 혼자 게임방 생성 (큐에 등록된 정보를 기반으로)
    public GameRoom createGameRoomForOneUser(Long userId) {

        // 사용자 조회
        NormalUser user = normalUserRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("유저를 찾을 수 없습니다."));

        // 현재 진행 중인 게임이나 다른 방 참여 여부 검사
        validateUsersBeforeMatch(List.of(userId), null);

        // 매칭 큐에서 등록된 정보 가져오기 (QUEUE_PRE만)
        MatchQueueEntry entry = matchQueueRepository.findByUser_UserIdAndMatchedFalse(userId)
                .stream()
                .filter(e -> e.getMatchType() == MatchQueueType.QUEUE_PRE)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("해당 유저는 사전 매칭 큐에 등록되어 있지 않습니다."));

        // 날짜/시간/위치 정보 추출
        LocalDate date = entry.getDate();
        LocalTime time = entry.getTime();
        Location location = entry.getLocation();

        if (location == null || date == null || time == null) {
            throw new RuntimeException("매칭 큐에 날짜, 시간 또는 위치 정보가 누락되었습니다.");
        }

        // 게임방 생성
        GameRoom room = new GameRoom();
        room.setLocation(location);
        room.setDate(date);
        room.setTime(time);
        room.setParticipants(List.of(user));  // 혼자 방 생성

        GameRoom savedRoom = gameRoomRepository.save(room);

        // 유저와 게임방 연결
        user.setGameRoom(savedRoom);
        normalUserRepository.save(user);

        return savedRoom;
    }

}
