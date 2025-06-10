package PL_25.shuttleplay.Controller;

import PL_25.shuttleplay.Entity.Game.*;
import PL_25.shuttleplay.Entity.User.NormalUser;
import PL_25.shuttleplay.Repository.MatchQueueRepository;
import PL_25.shuttleplay.Service.AutoMatchService;
import PL_25.shuttleplay.Service.MessageService;
import PL_25.shuttleplay.dto.Matching.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/match/auto")
@RequiredArgsConstructor
public class AutoMatchController {

    private final AutoMatchService autoMatchService;
    private final MatchQueueRepository matchQueueRepository;

    @Autowired(required = false)  // 선택적 주입
    private MessageService messageService;

    @Value("${message.enabled:false}")  // 메세지 서비스 api key 없을때 실행안되도록 설정값 추가
    private boolean messageEnabled;

    // 현장 구장 매칭 큐 등록 (게임 생성, 게임방 ID는 필수)(날짜/시간은 현재 기준, 위치는 게임방 기준)
    @PostMapping("/queue/gym")
    public ResponseEntity<Map<String, Object>> registerGymQueue(@RequestParam Long userId,
                                                                 @RequestParam Long roomId,
                                                                 @RequestBody AutoMatchRequest request) {
        MatchQueueResponse response = autoMatchService.registerToQueue(userId, roomId, request); // isPreMatch = false 내부 처리

        Map<String, Object> result = new HashMap<>();
        result.put("message", "매칭 큐 등록되었습니다.");
        result.put("isPrematched", response.isPrematched());
        result.put("userId", response.getUserId());
        result.put("gameRoomId", response.getGameRoomId());
        result.put("matchedUserCount", response.getMatchedUserCount());
        return ResponseEntity.ok(result);
    }

    // 사전 동네 또는 사전 구장 매칭 큐 등록 (게임방 생성)(직접 위치/날짜/시간 입력)
    @PostMapping("/queue/location-preGym")
    public ResponseEntity<Map<String, Object>> registerLocationQueue(@RequestParam Long userId,
                                                                      @RequestBody AutoMatchRequest request) {
        MatchQueueResponse response = autoMatchService.registerToQueue(userId, request); // isPreMatch = true 내부 처리

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

    // 매칭 큐 등록 취소(게임 시작 전)
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

    // 현장 자동 매칭 큐 사용자 조회 (자동 매칭용)
    @GetMapping("/queue-users")
    public ResponseEntity<Map<String, Object>> getAutoQueueUsersByRoom(@RequestParam Long roomId) {
        List<MatchQueueEntry> entries = matchQueueRepository
                .findByMatchedFalseAndGameRoom_GameRoomId(roomId).stream()
                .filter(entry -> entry.getMatchType() == MatchQueueType.QUEUE_LIVE_AUTO) // 자동 매칭 큐만 필터링
                .toList();

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

    // 구장 자동 매칭(사전/현장 게임방 다 적용)
    @PostMapping("/games/{roomId}")
    public ResponseEntity<Map<String, Object>> matchLiveInRoom(@PathVariable Long roomId) {
        Game game = autoMatchService.matchLiveCourtFromRoom(roomId);  // ✅ 매칭 시도 + 저장

        if (game == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "매칭 조건에 맞는 사용자가 부족합니다.");
        }

        // game으로 직접 DTO 생성
        GameDTO gameDto = convertToGameDto(game);

        // 매칭 완료하여 Game 생성 시, 해당 userId 에게 문자 보내기 (API 키 필요)
        // messageService가 null이 아닐 때, messageEnabled가 활성화 되어있을때 만 문자 전송
        // 참가자 목록도 null 아닐때 전송
        if (messageEnabled && messageService != null && game.getParticipants() != null) {
            for (GameParticipant participant : game.getParticipants()) {
                NormalUser user = participant.getUser();    // 게임참가자 리스트에 들어있는 개별 사용자 접근
                String to = user.getPhone();
                String text = "[셔틀플레이] " + user.getName() + "님! "
                        + game.getDate() + " " + game.getTime() + "에 "
                        + game.getLocation().getCourtName() + "에서 경기 매칭이 완료되었습니다!";
                messageService.sendMessage(to, text);
            }
        }

        return ResponseEntity.ok(Map.of(
                "message", "매칭 되었습니다.",
                "game", gameDto  // gameId, date, time, players 포함됨
        ));
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

    // 사전 동네 매칭(자동) - 유저가 대기열에 등록한 뒤, 날짜+시간+위치(300m 이내) 조건에 맞는 유저들과 게임방 자동 생성
    @PostMapping("/rooms/location")
    public ResponseEntity<Map<String, Object>> matchPreLocation(@RequestParam Long userId) {
        GameRoomDTO roomDto = autoMatchService.createPreLocationMeetingRoomDto(userId);

        if (roomDto == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "매칭 조건에 맞는 사용자가 부족합니다.");
        }

        return ResponseEntity.ok(Map.of(
                "message", "게임방이 생성되었습니다.",
                "room", roomDto
        ));
    }

    // 사전 구장 매칭(자동) - 유저가 대기열에 등록한 뒤, 동일 구장/날짜/시간 조건에 맞는 유저들과 게임방 자동 생성
    @PostMapping("/rooms/gym")
    public ResponseEntity<Map<String, Object>> matchPreGym(@RequestParam Long userId) {
        GameRoomDTO roomDto = autoMatchService.createPreGymMeetingRoomDto(userId);

        if (roomDto == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "매칭 조건에 맞는 사용자가 부족합니다.");
        }

        return ResponseEntity.ok(Map.of(
                "message", "게임방이 생성되었습니다.",
                "room", roomDto
        ));
    }

}