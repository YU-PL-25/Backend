package PL_25.shuttleplay.Controller;

import PL_25.shuttleplay.Service.GameRoom.GameRoomPutService;
import PL_25.shuttleplay.dto.GameRoomPutDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class GameRoomApiController {

    private final GameRoomPutService gameRoomPutService;


    // 게임방 생성 요청.
    /*
        방 생성 성공 => CREATED(201) 반환.
        방 생성 실패 => BAD_REQUEST(400) 반환.
    */
    @PostMapping("/game-room")
    public ResponseEntity<Void> postGameRoom(@RequestBody GameRoomPutDTO gameRoomPutDTO) {

        // db에 저장 했는지 성공 여부 반환.
        boolean result = gameRoomPutService.putGameRoom(gameRoomPutDTO);

        // 성공 했으면 상태코드 201 반환.
        if (result) {
            return ResponseEntity.status(HttpStatus.CREATED).build();
        }
        // 실패 했으면 상태코드 400 반환.
        else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }
}
