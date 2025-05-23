package PL_25.shuttleplay.Controller;

import PL_25.shuttleplay.dto.Matching.CurrentMatchingGameRoomDTO;
import PL_25.shuttleplay.dto.Matching.PreMatchingGameRoomDTO;
import PL_25.shuttleplay.Entity.Game.GameRoom;
import PL_25.shuttleplay.Service.GameRoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class GameRoomApiController {

    private final GameRoomService gameRoomService;


    // 게임방 생성 요청.
    /*
        방 생성 성공 => CREATED(201) 반환.
        방 생성 실패 => BAD_REQUEST(400) 반환.
    */

    // POST 현장 매칭(구장) 생성
    @PostMapping("/api/game-room/current-matching")
    public ResponseEntity<Map<String, Object>> postCurrentMatchingGameRoom(
            @RequestBody CurrentMatchingGameRoomDTO gameRoomDTO) {

        GameRoom gameRoom = gameRoomService.putCurrentMatchingGameRoom(gameRoomDTO);

        Map<String, Object> response = new HashMap<>();
        if (gameRoom != null) {
            response.put("status", 200);
            response.put("message", "현장 매칭(구장) 게임방이 성공적으로 생성되었습니다.");
            response.put("gameRoomId", gameRoom.getGameRoomId());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } else {
            response.put("status", 400);
            response.put("error", "현장 매칭(구장) 게임방 생성에 실패했습니다.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }


    // POST 사전 매칭(구장) 생성
    @PostMapping("/api/game-room/pre-matching")
    public ResponseEntity<Map<String, Object>> postPreMatchingGameRoom(
            @RequestBody PreMatchingGameRoomDTO gameRoomDTO
            ) {

        // db에 저장 했는지 성공 여부 반환.
        GameRoom gameRoom = gameRoomService.putPreMatchingGameRoom(gameRoomDTO);

        Map<String, Object> response = new HashMap<>();
        if (gameRoom != null) {
            response.put("status", 200);
            response.put("message", "사전 매칭(구장) 게임방이 성공적으로 생성되었습니다.");
            response.put("gameRoomId", gameRoom.getGameRoomId());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } else {
            response.put("status", 400);
            response.put("error", "사전 매칭(구장) 게임방 생성에 실패했습니다.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }
}
