package PL_25.shuttleplay.Controller;

import PL_25.shuttleplay.dto.Matching.AutoMatchRequest;
import PL_25.shuttleplay.Entity.Game.Game;
import PL_25.shuttleplay.Entity.Game.GameRoom;
import PL_25.shuttleplay.Entity.Game.MatchQueueResponse;
import PL_25.shuttleplay.Entity.User.NormalUser;
import PL_25.shuttleplay.Service.AutoMatchService;
import PL_25.shuttleplay.Service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/match/auto")
@RequiredArgsConstructor
public class AutoMatchController {

    private final AutoMatchService autoMatchService;
    private final MessageService messageService;

    @PostMapping("/queue/gym")
    public ResponseEntity<Map<String, Object>> registerGymQueue(@RequestParam Long userId,
                                                                 @RequestBody AutoMatchRequest request) {
        request.setDate(null);
        request.setTime(null);
        MatchQueueResponse response = autoMatchService.registerToQueue(userId, request);
        Map<String, Object> result = new HashMap<>();
        result.put("message", "매칭 큐 등록되었습니다.");
        result.put("userId", response.getUserId());
        if (response.getGameRoomId() != null) {
            result.put("gameRoomId", response.getGameRoomId());
        }
        return ResponseEntity.ok(result);
    }

    @PostMapping("/queue/location")
    public ResponseEntity<Map<String, Object>> registerLocationQueue(@RequestParam Long userId,
                                                                      @RequestBody AutoMatchRequest request) {
        MatchQueueResponse response = autoMatchService.registerToQueue(userId, request);
        Map<String, Object> result = new HashMap<>();
        result.put("message", "매칭 큐 등록되었습니다.");
        result.put("userId", response.getUserId());
        result.put("location", Map.of(
                "courtName", response.getCourtName(),
                "courtAddress", response.getCourtAddress()
        ));
        result.put("date", response.getDate());
        result.put("time", response.getTime());
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/queue")
    public ResponseEntity<Map<String, Object>> cancelQueue(@RequestParam Long userId) {
        autoMatchService.cancelQueueEntry(userId);
        return ResponseEntity.ok(
                Map.of(
                        "message", "매칭 등록이 성공적으로 취소되었습니다.",
                        "timestamp", LocalDateTime.now()
                )
        );
    }

    @PostMapping("/pre-court/{roomId}")
    public ResponseEntity<Map<String, Object>> matchPreCourtInRoom(@PathVariable Long roomId) {
        Game game = autoMatchService.matchPreCourtFromRoom(roomId);
        if (game == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "매칭 조건에 맞는 사용자가 부족합니다.");
        }

//        // 매칭 완료하여 Game 생성 시, 해당 userId 에게 문자 보내기
//        for (NormalUser user : game.getParticipants()) {
//            String to = user.getPhone();
//            String text = "[셔틀플레이] " + user.getName() + "님! "
//                    + game.getDate() + " " + game.getTime() + "에 "
//                    + game.getLocation().getCourtName() + "에서 경기 매칭이 완료되었습니다!";
//            messageService.sendMessage(to, text);
//        }

        return ResponseEntity.ok(Map.of(
                "message", "매칭 되었습니다.",
                "gameId", game.getGameId(),
                "userIds", game.getParticipants().stream().map(u -> u.getUserId()).toList(),
                "location", game.getLocation(),
                "date", game.getDate(),
                "time", game.getTime()
        ));
    }

    @PostMapping("/live/{roomId}")
    public ResponseEntity<Map<String, Object>> matchLiveInRoom(@PathVariable Long roomId) {
        Game game = autoMatchService.matchLiveCourtFromRoom(roomId);
        if (game == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "매칭 조건에 맞는 사용자가 부족합니다.");
        }

        return ResponseEntity.ok(Map.of(
                "message", "매칭 되었습니다.",
                "gameId", game.getGameId(),
                "userIds", game.getParticipants().stream().map(u -> u.getUserId()).toList(),
                "location", game.getLocation(),
                "date", game.getDate(),
                "time", game.getTime()
        ));
    }

    @PostMapping("/pre-location")
    public ResponseEntity<Map<String, Object>> matchPreLocation(@RequestParam Long userId,
                                                                 @RequestBody AutoMatchRequest request) {
        GameRoom room = autoMatchService.createPreLocationMeetingRoomFromUser(
                userId,
                request.getDate(),
                request.getTime(),
                request.getLocation()
        );
        if (room == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "매칭 조건에 맞는 사용자가 부족합니다.");
        }
        return ResponseEntity.ok(Map.of(
                "message", "매칭 되었습니다.",
                "gameRoomId", room.getGameRoomId(),
                "userIds", room.getParticipants().stream().map(u -> u.getUserId()).toList(),
                "location", room.getLocation(),
                "date", room.getDate(),
                "time", room.getTime()
        ));
    }
}
