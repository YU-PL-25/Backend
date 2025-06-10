package PL_25.shuttleplay.Controller;

import PL_25.shuttleplay.Entity.Game.*;
import PL_25.shuttleplay.Entity.User.NormalUser;
import PL_25.shuttleplay.Repository.GameRoomRepository;
import PL_25.shuttleplay.Repository.MatchQueueRepository;
import PL_25.shuttleplay.Repository.NormalUserRepository;
import PL_25.shuttleplay.Service.ManualMatchService;
import PL_25.shuttleplay.dto.Matching.GameDTO;
import PL_25.shuttleplay.dto.Matching.GameRoomDTO;
import PL_25.shuttleplay.dto.Matching.ManualMatchRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
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
                                                                @RequestParam Long roomId,
                                                                @RequestBody ManualMatchRequest request) {
        request.setDate(null);
        request.setTime(null);
        MatchQueueResponse response = manualMatchService.registerToQueue(userId, roomId, request);

        Map<String, Object> result = new HashMap<>();
        result.put("message", "매칭 큐 등록되었습니다.");
        result.put("userId", response.getUserId());
        result.put("isPrematched", response.isPrematched());
        if (response.getGameRoomId() != null) {
            result.put("gameRoomId", response.getGameRoomId());
        }
        return ResponseEntity.ok(result);
    }

    // 매칭 큐 취소
    @DeleteMapping("/queue")
    public ResponseEntity<Map<String, Object>> cancelQueue(@RequestParam Long userId) {
        manualMatchService.cancelQueueEntry(userId);
        return ResponseEntity.ok(Map.of(
                        "message", "매칭 등록이 성공적으로 취소되었습니다.",
                        "timestamp", LocalDateTime.now()
                ));
    }

    // 특정 게임방에 등록된 매칭 큐 사용자 목록 조회 (수동 매칭용)
    @GetMapping("/queue-users")
    public ResponseEntity<Map<String, Object>> getUsersInQueueByRoom(@RequestParam Long roomId) {
        // 해당 게임방 ID 기준으로 큐에서 아직 매칭되지 않은 유저 목록 조회
        List<MatchQueueEntry> entries = matchQueueRepository
                .findByMatchedFalseAndGameRoom_GameRoomId(roomId).stream()
                .filter(entry -> entry.getMatchType() == MatchQueueType.QUEUE_LIVE_MANUAL)
                .toList();

        // 필요한 유저 정보만 추려서 반환
        List<Map<String, Object>> userList = entries.stream().map(entry -> {
            NormalUser user = entry.getUser();
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("userId", user.getUserId());
            userInfo.put("nickname", user.getNickname());
            userInfo.put("rank", user.getRank());
            return userInfo;
        }).toList();

        return ResponseEntity.ok(Map.of(
                "roomId", roomId,
                "queuedUsers", userList
        ));
    }

    // 구장 수동 매칭(사전/현장 게임방 다 적용) - 게임방 내 큐에 등록된 사람들 중 manager 역할의 사용자 직접 매칭 수행
    @PostMapping("/games/{roomId}")
    public ResponseEntity<Map<String, Object>> createLiveGame(@PathVariable Long roomId,
                                                              @RequestParam Long requesterId,
                                                              @RequestBody Map<String, List<Long>> body) {
        // 매칭 대상 유저 ID 목록 추출
        // 컨트롤러에서 방장 ID 제외하고 넘김
        List<Long> userIds = body.get("userIds")
            .stream()
            .filter(id -> !id.equals(requesterId))  // 방장 제거
            .toList();

        // 게임방 조회
        GameRoom room = manualMatchService.getGameRoomById(roomId);

        // 수동 매칭 실행 (requesterId를 통해 방장인지 검증)
        GameDTO gameDto = manualMatchService.createLiveGameFromRoomDto(room, userIds, requesterId);

        // 응답 반환 (현재 시각 기준 경기 생성, managerId 포함)
        return ResponseEntity.ok(Map.of(
                "message", "매칭 되었습니다.",
                "game", gameDto,
                "managerId", requesterId
        ));
    }

    // =====================사전 동네=======================
    // 사전 수동 매칭 (동네 기준) - 매칭 큐 등록 + 300m 이내 게임방 리스트 반환
    @PostMapping("/rooms/location")
    public ResponseEntity<Map<String, Object>> createLocationRoom(@RequestParam Long userId,
                                                                   @RequestBody ManualMatchRequest request) {
        // 매칭 큐 등록 + 주변 게임방 조회 통합 처리
        List<GameRoomDTO> rooms = manualMatchService.registerQueueAndFindNearbyRoomsDto(userId, request);

        return ResponseEntity.ok(Map.of(
                "message", "매칭 큐 등록 및 주변 게임방 조회 완료",
                "userId", userId,
                "rooms", rooms
        ));
    }

    // 사전 수동 매칭 (동네 기준) - 방 목록에서 게임방 참가
    @PostMapping("/rooms/location/{roomId}/join")
    public ResponseEntity<Map<String, Object>> joinRoom(@PathVariable Long roomId,
                                                        @RequestParam Long userId) {
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

    // 사전 수동 매칭 (동네 기준) - 큐에 등록된 정보 기반으로 방 목록에서 새로운 게임방 생성
    @PostMapping("/rooms/location/create")
    public ResponseEntity<Map<String, Object>> createLocationRoom(@RequestParam Long userId) {

        // 매칭 큐에서 사용자 정보 기반으로 게임방 생성
        GameRoomDTO roomDto = manualMatchService.createGameRoomForOneUserDto(userId);

        return ResponseEntity.ok(Map.of(
                "message", "큐 기반 게임방 생성 완료",
                "room", roomDto
        ));
    }

    // =====================사전 구장=======================
    // 사전 수동 매칭 (구장 기준) - 매칭 큐 등록 + 동일 구장 게임방 리스트 반환
    @PostMapping("/rooms/gym")
    public ResponseEntity<Map<String, Object>> createGymRoom(@RequestParam Long userId,
                                                              @RequestBody ManualMatchRequest request) {
        // 매칭 큐 등록 + 동일 구장 게임방 조회 통합 처리
        List<GameRoomDTO> rooms = manualMatchService.registerQueueAndFindMatchingRoomsDto(userId, request);

        return ResponseEntity.ok(Map.of(
                "message", "매칭 큐 등록 및 동일 구장 게임방 조회 완료",
                "userId", userId,
                "rooms", rooms
        ));
    }

    // 사전 수동 매칭 (구장 기준) - 기존 구장 게임방에 사용자 참여 (큐에 등록된 상태여야 함)
    @PostMapping("/rooms/gym/{roomId}/join")
    public ResponseEntity<Map<String, Object>> joinGymRoom(@PathVariable Long roomId,
                                                           @RequestParam Long userId) {
        // 게임방 및 사용자 조회
        GameRoom room = gameRoomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("방을 찾을 수 없습니다."));
        NormalUser user = normalUserRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("유저를 찾을 수 없습니다."));

        // 사용자 상태 검증 (다른 방, 게임 중 등)
        manualMatchService.validateUsersBeforeMatch(List.of(userId), room);

        // 매칭 큐 조회 및 처리
        List<MatchQueueEntry> entries = matchQueueRepository.findByUser_UserIdAndMatchedFalse(userId);
        if (entries.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "해당 유저의 매칭 큐가 존재하지 않습니다.");
        }
        for (MatchQueueEntry entry : entries) {
            entry.setMatched(true);       // 매칭 완료 처리
            entry.setGameRoom(room);      // 게임방 연결
        }
        matchQueueRepository.saveAll(entries);

        // 유저를 게임방 참가자로 등록
        room.getParticipants().add(user);
        user.setGameRoom(room);
        gameRoomRepository.save(room);
        normalUserRepository.save(user);

        return ResponseEntity.ok(Map.of(
                "message", "구장 게임방 참가 완료",
                "gameRoomId", roomId
        ));
    }

    // 사전 수동 매칭 (구장 기준) - 큐에 등록된 정보 기반으로 새로운 게임방 생성
    @PostMapping("/rooms/gym/create")
    public ResponseEntity<Map<String, Object>> createGymRoom(@RequestParam Long userId) {

        // 매칭 큐에서 사용자 정보 기반으로 게임방 생성 (구장 정보 기반)
        GameRoomDTO roomDto = manualMatchService.createGameRoomForOneUserDto(userId);

        return ResponseEntity.ok(Map.of(
                "message", "큐 기반 구장 게임방 생성 완료",
                "room", roomDto
        ));
    }
}