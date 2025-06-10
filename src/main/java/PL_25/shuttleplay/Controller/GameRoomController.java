package PL_25.shuttleplay.Controller;

import PL_25.shuttleplay.Entity.Game.Game;
import PL_25.shuttleplay.Entity.Game.GameRoom;
import PL_25.shuttleplay.Service.GameRoomService;
import PL_25.shuttleplay.dto.Matching.CurrentMatchingGameRoomDTO;
import PL_25.shuttleplay.dto.Matching.GameRoomDTO;
import PL_25.shuttleplay.dto.Matching.GameRoomJoinDTO;
import PL_25.shuttleplay.dto.Matching.PreMatchingGameRoomDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@RequiredArgsConstructor
public class GameRoomController {

    private final GameRoomService gameRoomService;


    // 게임방 생성 요청.
    /*
        방 생성 성공 => OK(200) 반환.
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
            response.put("managerId", gameRoom.getCreatedBy().getUserId()); // 방장 id 같이 반환
            return ResponseEntity.status(HttpStatus.OK).body(response);
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
            response.put("managerId", gameRoom.getCreatedBy().getUserId()); // 방장 id 같이 반환
            return ResponseEntity.status(HttpStatus.OK).body(response);
        } else {
            response.put("status", 400);
            response.put("error", "사전 매칭(구장) 게임방 생성에 실패했습니다.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }


    // 이미 생성되어 있는 방에 참가 요청
    @PostMapping("/api/rooms/{roomId}/join")
    public ResponseEntity<Map<String, Object>> joinGameRoom(
            @PathVariable("roomId") long roomId,
            @RequestBody GameRoomJoinDTO gameRoomJoinDTO
            ) {

        Map<String, Object> response = new HashMap<>();

        try {
            GameRoom gameRoom = gameRoomService.addUserToGameRoom(roomId, gameRoomJoinDTO.getUserId());

            response.put("status", 200);
            response.put("message", "유저의 게임방 참가 요청이 성공했습니다.");
            response.put("gameRoomId", gameRoom.getGameRoomId());
            return ResponseEntity.status(HttpStatus.OK).body(response);

        } catch (NoSuchElementException e) {
            response.put("status", 400);
            response.put("error", "유저의 게임방 참가 요청이 실패했습니다.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }


    // 유저가 참가한 게임방 나가기.
    @DeleteMapping("/api/users/{userId}/game-room")
    public ResponseEntity<Map<String, Object>> leaveGameRoom(
            @PathVariable("userId") long userId
    ) {

        Map<String, Object> response = new HashMap<>();

        try {

            GameRoom gameRoom = gameRoomService.leaveGameRoom(userId);

            response.put("status", 200);
            response.put("message", "유저의 게임방 나가기가 성공했습니다.");
            response.put("gameRoomId", gameRoom.getGameRoomId());
            return ResponseEntity.status(HttpStatus.OK).body(response);

        } catch (NoSuchElementException e) {
            response.put("status", 400);
            response.put("error", "유저의 게임방 나가기가 실패했습니다.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }


    // 게임방 삭제.
    @DeleteMapping("/api/game-room/{gameRoomId}")
    public ResponseEntity<Map<String, Object>> deleteGameRoom(
            @PathVariable("gameRoomId") long gameRoomId
    ) {

        Map<String, Object> response = new HashMap<>();

        try {

            gameRoomService.deleteGameRoom(gameRoomId);

            response.put("status", 200);
            response.put("message", "게임방 삭제에 성공했습니다.");
            response.put("gameRoomId", gameRoomId);
            return ResponseEntity.status(HttpStatus.OK).body(response);

        } catch (NoSuchElementException e) {

            response.put("status", 400);
            response.put("error", "게임방 삭제에 실패했습니다.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }


    // 게임방 전체 조회
    @GetMapping("/api/game-room")
    public ResponseEntity<Map<String, Object>> selectAllGameRoom() {

        Map<String, Object> response = new HashMap<>();

        try {

            List<GameRoomDTO> gameRoomList = gameRoomService.selectAllGameRoom();
            
            response.put("status", 200);
            response.put("message", "게임방 전체 조회에 성공했습니다.");
            response.put("data", gameRoomList);
            return ResponseEntity.status(HttpStatus.OK).body(response);

        } catch (NoSuchElementException e) {

            response.put("status", 400);
            response.put("error", "게임방 전체 조회에 실패했습니다.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }


    // 같은 구장에 있는 게임방 조회.
    @GetMapping("/api/locations/{locationId}/game-room")
    public ResponseEntity<Map<String, Object>> selectGameRoomByLocation(
            @PathVariable("locationId") long locationId
    ) {

        Map<String, Object> response = new HashMap<>();

        try {

            List<GameRoom> gameRoomList = gameRoomService.selectGameRoomByLocation(locationId);

            response.put("status", 200);
            response.put("message", "구장 기준으로 게임방 조회에 성공했습니다.");
            response.put("data", gameRoomList);
            return ResponseEntity.status(HttpStatus.OK).body(response);
        } catch (NoSuchElementException e) {

            response.put("status", 400);
            response.put("error", "구장 기준으로 게임방 조회에 실패했습니다.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    // 하나의 GameRoom에서 생성된 Game 리스트 조회
    @GetMapping("/api/game-room/{gameRoomId}/game-list")
    public ResponseEntity<Map<String, Object>> getGamesInGameRoom(@PathVariable Long gameRoomId) {
        // DTO 형태로 반환
        GameRoomDTO dto = gameRoomService.getGamesByGameRoomId(gameRoomId);

        Map<String, Object> response = new HashMap<>();
        response.put("status", 200);
        response.put("message", "게임방 및 게임 리스트 조회 성공");
        response.put("data", dto);

        return ResponseEntity.ok(response);
    }


    // 게임방id로 상세정보 조회하기
    @GetMapping("/api/game-room/{gameRoomId}")
    public ResponseEntity<Map<String, Object>> selectGameRoom(
            @PathVariable("gameRoomId") Long gameRoomId
    ) {

        Map<String, Object> response = new HashMap<>();

        try {

            GameRoom gameRoom = gameRoomService.selectGameRoomById(gameRoomId);
            response.put("status", 200);
            response.put("message", "게임방 id : " + gameRoomId + ", 기준으로 조회에 성공했습니다.");
            response.put("data", gameRoom);
            return ResponseEntity.status(HttpStatus.OK).body(response);
        } catch (Exception e) {
            response.put("status", 400);
            response.put("message", "게임방 id : " + gameRoomId + ", 기준으로 조회에 실패했습니다.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

}
