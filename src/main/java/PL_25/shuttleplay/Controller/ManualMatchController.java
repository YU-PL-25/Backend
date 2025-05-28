package PL_25.shuttleplay.Controller;

import PL_25.shuttleplay.Entity.User.NormalUser;
import PL_25.shuttleplay.Repository.GameRoomRepository;
import PL_25.shuttleplay.Repository.MatchQueueRepository;
import PL_25.shuttleplay.Repository.NormalUserRepository;
import PL_25.shuttleplay.dto.Matching.ManualMatchRequest;
import PL_25.shuttleplay.Entity.Game.Game;
import PL_25.shuttleplay.Entity.Game.GameRoom;
import PL_25.shuttleplay.Entity.Game.MatchQueueEntry;
import PL_25.shuttleplay.Entity.Game.MatchQueueResponse;
import PL_25.shuttleplay.Service.ManualMatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
@RestController
@RequestMapping("/api/match/manual")
@RequiredArgsConstructor
public class ManualMatchController {

    private final ManualMatchService manualMatchService;
    private final GameRoomRepository gameRoomRepository;
    private final NormalUserRepository normalUserRepository;
    private final MatchQueueRepository matchQueueRepository;

    // 구장 기반 큐 등록
    @PostMapping("/queue/gym")
    public ResponseEntity<Map<String, Object>> registerGymQueue(@RequestParam Long userId,
                                                                @RequestBody ManualMatchRequest request) {
        request.setDate(null);
        request.setTime(null);
        MatchQueueResponse response = manualMatchService.registerToQueue(userId, request);

        Map<String, Object> result = new HashMap<>();
        result.put("message", "매칭 큐 등록되었습니다.");
        result.put("userId", response.getUserId());
        result.put("isPrematched", response.isPrematched());
        if (response.getGameRoomId() != null) {
            result.put("gameRoomId", response.getGameRoomId());
        }
        return ResponseEntity.ok(result);
    }

    // 동네 기반 큐 등록
    @PostMapping("/queue/location")
    public ResponseEntity<Map<String, Object>> registerLocationQueue(@RequestParam Long userId,
                                                                     @RequestBody ManualMatchRequest request) {
        MatchQueueResponse response = manualMatchService.registerToQueue(userId, request);

        Map<String, Object> result = new HashMap<>();
        result.put("message", "매칭 큐 등록되었습니다.");
        result.put("userId", response.getUserId());
        result.put("isPrematched", response.isPrematched());
        result.put("courtName", response.getCourtName());
        result.put("courtAddress", response.getCourtAddress());
        result.put("date", response.getDate());
        result.put("time", response.getTime());
        return ResponseEntity.ok(result);
    }

    // 매칭 큐 취소
    @DeleteMapping("/queue")
    public ResponseEntity<Map<String, Object>> cancelQueue(@RequestParam Long userId) {
        manualMatchService.cancelQueueEntry(userId);
        return ResponseEntity.ok(Map.of("message", "취소 완료", "status", 200));
    }

    // 사전 수동 매칭 (구장 기준) - 게임방 내 큐에 등록된 사람들 중 manager 역할의 사용자 직접 매칭 수행
    @PostMapping("/create/manual-game")
    public ResponseEntity<Map<String, Object>> createManualGame(@RequestParam Long roomId,
                                                                @RequestParam Long requesterId,
                                                                @RequestBody Map<String, Object> body) {
        // 1. 게임방 조회
        GameRoom room = manualMatchService.getGameRoomById(roomId);

        // 2. 요청 바디에서 사용자 ID 목록과 날짜/시간 추출
        List<Integer> userIds = (List<Integer>) body.get("userIds");
        String dateStr = (String) body.get("date");
        String timeStr = (String) body.get("time");

        // 3. 문자열 → LocalDate/LocalTime 변환
        LocalDate date = (dateStr != null) ? LocalDate.parse(dateStr) : null;
        LocalTime time = (timeStr != null) ? LocalTime.parse(timeStr) : null;

        // 4. 사용자 ID 정수형 리스트 → Long 타입 변환
        List<Long> longUserIds = userIds.stream().map(Integer::longValue).toList();

        // 5. 수동 매칭 실행 (요청자 ID를 통해 방장인지 검증)
        Game game = manualMatchService.createManualGameFromRoom(room, longUserIds, date, time, requesterId);

        // 6. 응답 반환 (managerId 포함)
        return ResponseEntity.ok(Map.of(
                "message", "매칭 되었습니다.",
                "gameId", game.getGameId(),
                "userIds", longUserIds,
                "managerId", requesterId,
                "location", game.getLocation(),
                "date", game.getDate(),
                "time", game.getTime()
        ));
    }

    // 현장 수동 매칭(구장 기준) - 게임방 내 큐에 등록된 사람들 중 manager 역할의 사용자 직접 매칭 수행
    @PostMapping("/create/live-game")
    public ResponseEntity<Map<String, Object>> createLiveGame(@RequestParam Long roomId,
                                                              @RequestParam Long requesterId,
                                                              @RequestBody Map<String, List<Long>> body) {
        // 1. 매칭 대상 유저 ID 목록 추출
        List<Long> userIds = body.get("userId");

        // 2. 게임방 조회
        GameRoom room = manualMatchService.getGameRoomById(roomId);

        // 3. 수동 매칭 실행 (requesterId를 통해 방장인지 검증)
        Game game = manualMatchService.createLiveGameFromRoom(room, userIds, requesterId);

        // 4. 응답 반환 (현재 시각 기준 경기 생성, managerId 포함)
        return ResponseEntity.ok(Map.of(
                "message", "매칭 되었습니다.",
                "gameId", game.getGameId(),
                "userIds", userIds,
                "managerId", requesterId,
                "location", game.getLocation(),
                "date", game.getDate(),
                "time", game.getTime()
        ));
    }

    // 사전 수동 매칭(동네 기준) - 매칭 큐 등록 + 근처 게임방 리스트 반환
    @PostMapping("/queue/location-and-rooms")
    public ResponseEntity<Map<String, Object>> registerAndGetNearbyRooms(@RequestParam Long userId,
                                                                         @RequestBody ManualMatchRequest request) {
        // 1. 큐 등록
        MatchQueueResponse response = manualMatchService.registerToQueue(userId, request);

        // 2. 300m 이내 동일 시간대 게임방 검색
        List<GameRoom> nearbyRooms = manualMatchService.findNearbyRooms(
                request.getLocation().getLatitude(),
                request.getLocation().getLongitude(),
                request.getDate(),
                request.getTime()
        );

        return ResponseEntity.ok(Map.of(
                "message", "큐 등록 및 주변 게임방 조회 완료",
                "registeredUserId", response.getUserId(),
                "rooms", nearbyRooms
        ));
    }

    // 사전 수동 매칭 (동네 기준) - 방 목록에서 게임방 참가
    @PostMapping("/join-room")
    public ResponseEntity<Map<String, Object>> joinRoom(@RequestParam Long userId, @RequestParam Long roomId) {
        GameRoom room = gameRoomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("방을 찾을 수 없습니다."));
        NormalUser user = normalUserRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("유저를 찾을 수 없습니다."));

        // 유효성 검사
        manualMatchService.validateUsersBeforeMatch(List.of(userId), room);

        // 매칭 큐 entry 조회 및 matched 처리
        List<MatchQueueEntry> entries = matchQueueRepository.findByUser_UserIdAndMatchedFalse(userId);
        if (entries.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "해당 유저의 매칭 큐가 존재하지 않습니다.");
        }
        for (MatchQueueEntry entry : entries) {
            entry.setMatched(true);
            entry.setGameRoom(room); // 방 정보도 설정해 줌
        }
        matchQueueRepository.saveAll(entries); // 저장

        // 게임방에 참가자 추가
        room.getParticipants().add(user);
        user.setGameRoom(room);
        gameRoomRepository.save(room);
        normalUserRepository.save(user);

        return ResponseEntity.ok(Map.of(
                "message", "게임방 참가 완료",
                "gameRoomId", roomId
        ));
    }
    // 사전 수동 매칭 (동네 기준)
    // 방 목록에서 새로운 게임방 생성
    @PostMapping("/create/location-room")
    public ResponseEntity<Map<String, Object>> createLocationRoom(@RequestBody Map<String, Object> body) {
        // 장소 정보 (이름, 주소)
        String courtName = (String) body.get("courtName");
        String courtAddress = (String) body.get("courtAddress");

        // 좌표 정보 (반드시 있어야 거리 계산 가능)
        double latitude = ((Number) body.get("latitude")).doubleValue();
        double longitude = ((Number) body.get("longitude")).doubleValue();

        // 경기 날짜 / 경기 시간
        LocalDate date = LocalDate.parse((String) body.get("date"));
        LocalTime time = LocalTime.parse((String) body.get("time"));

        // 방 생성 요청한 사용자 ID
        Long userId = ((Number) body.get("userId")).longValue();

        // 실제 게임방 생성 서비스 호출
        GameRoom room = manualMatchService.createGameRoomForOneUser(
                courtName, courtAddress, latitude, longitude, date, time, userId
        );

        // 클라이언트에 생성 결과 응답
        return ResponseEntity.ok(Map.of(
                "message", "새 게임방 생성 완료",
                "gameRoomId", room.getGameRoomId(),
                "userId", userId,
                "location", room.getLocation(),
                "date", room.getDate(),
                "time", room.getTime()
        ));
    }
}