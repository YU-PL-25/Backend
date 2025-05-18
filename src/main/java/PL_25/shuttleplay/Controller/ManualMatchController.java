package PL_25.shuttleplay.Controller;

import PL_25.shuttleplay.Dto.Matching.ManualMatchRequest;
import PL_25.shuttleplay.Entity.Game.Game;
import PL_25.shuttleplay.Entity.Game.GameRoom;
import PL_25.shuttleplay.Entity.Game.MatchQueueEntry;
import PL_25.shuttleplay.Entity.Game.MatchQueueResponse;
import PL_25.shuttleplay.Service.ManualMatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    // 사전 수동 매칭 (구장 기준)
    @PostMapping("/create/manual-game")
    public ResponseEntity<Map<String, Object>> createManualGame(@RequestParam Long roomId,
                                                                @RequestBody Map<String, Object> body) {
        GameRoom room = manualMatchService.getGameRoomById(roomId);

        List<Integer> userIds = (List<Integer>) body.get("userIds");
        String dateStr = (String) body.get("date");
        String timeStr = (String) body.get("time");

        LocalDate date = (dateStr != null) ? LocalDate.parse(dateStr) : null;
        LocalTime time = (timeStr != null) ? LocalTime.parse(timeStr) : null;


        List<Long> longUserIds = userIds.stream().map(Integer::longValue).toList();
        Game game = manualMatchService.createManualGameFromRoom(room, longUserIds, date, time);

        return ResponseEntity.ok(Map.of(
                "message", "매칭 되었습니다.",
                "gameId", game.getGameId(),
                "userIds", longUserIds,
                "location", game.getLocation(),
                "date", game.getDate(),
                "time", game.getTime()
        ));
    }
    // 현장 수동 매칭(구장 기준)
    @PostMapping("/create/live-game")
    public ResponseEntity<Map<String, Object>> createLiveGame(@RequestParam Long roomId,
                                                              @RequestBody Map<String, List<Long>> body) {
        List<Long> userIds = body.get("userId");
        GameRoom room = manualMatchService.getGameRoomById(roomId);
        Game game = manualMatchService.createLiveGameFromRoom(room, userIds);

        return ResponseEntity.ok(Map.of(
                "message", "매칭 되었습니다.",
                "gameId", game.getGameId(),
                "userIds", userIds,
                "location", game.getLocation(),
                "date", game.getDate(),
                "time", game.getTime()
        ));
    }

    // 사전 수동매칭에 사용할 거리 기반으로 가까운 유저 반환
    @GetMapping("/queue/nearby")
    public ResponseEntity<List<MatchQueueEntry>> getNearbyUsers(@RequestParam double latitude,
                                                                 @RequestParam double longitude) {
        List<MatchQueueEntry> nearby = manualMatchService.getNearbyPreLocationQueue(latitude, longitude);
        return ResponseEntity.ok(nearby);
    }

    // 사전 수동 매칭 (동네 기준)
    @PostMapping("/create/location-room")
    public ResponseEntity<Map<String, Object>> createLocationRoom(@RequestBody Map<String, Object> body) {
        String courtName = (String) body.get("courtName");
        String courtAddress = (String) body.get("courtAddress");
        LocalDate date = LocalDate.parse((String) body.get("date"));
        LocalTime time = LocalTime.parse((String) body.get("time"));
        List<Integer> userIds = (List<Integer>) body.get("userIds");
        List<Long> longUserIds = userIds.stream().map(Integer::longValue).toList();

        GameRoom room = manualMatchService.createGameRoomFromUserList(courtName, courtAddress, date, time, longUserIds);

        return ResponseEntity.ok(Map.of(
                "message", "매칭 되었습니다.",
                "gameRoomId", room.getGameRoomId(),
                "userIds", longUserIds,
                "location", room.getLocation(),
                "date", room.getDate(),
                "time", room.getTime()
        ));
    }
}