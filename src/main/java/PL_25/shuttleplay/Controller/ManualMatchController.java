package PL_25.shuttleplay.Controller;

import PL_25.shuttleplay.Dto.Matching.ManualMatchRequest;
import PL_25.shuttleplay.Entity.Game.Game;
import PL_25.shuttleplay.Entity.Game.GameRoom;
import PL_25.shuttleplay.Repository.GameRoomRepository;
import PL_25.shuttleplay.Service.ManualMatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/match/manual")
@RequiredArgsConstructor
public class ManualMatchController {

    private final ManualMatchService manualMatchService;
    private final GameRoomRepository gameRoomRepository;

    // 사전 수동 매칭(구장 기준): 큐에 등록된 유저들 중 선택해서 게임 생성
    @PostMapping("/pre-court/{roomId}")
    public Game manualPreCourt(@PathVariable Long roomId, @RequestBody ManualMatchRequest request) {
        GameRoom room = gameRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("게임방을 찾을 수 없습니다."));

        return manualMatchService.createManualGameFromRoom(
                room,
                request.getUserId(),
                request.getDate(),
                request.getTime()
        );
    }

    // 현장 수동 매칭(구장 기준): 큐에 등록된 유저들 중 선택, 현재 시간으로 게임 생성
    @PostMapping("/live/{roomId}")
    public Game manualLive(@PathVariable Long roomId, @RequestBody ManualMatchRequest request) {
        GameRoom room = gameRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("게임방을 찾을 수 없습니다."));

        return manualMatchService.createLiveGameFromRoom(
                room,
                request.getUserId()
        );
    }

    // 사전 수동 매칭(동네 기준): 큐 전체에서 미매칭 유저로 게임방 생성
    @PostMapping("/pre-location")
    public GameRoom manualPreLocation(@RequestBody ManualMatchRequest request) {
        return manualMatchService.createManualMeetingRoomFromQueue(
                request.getCourtName(),
                request.getCourtAddress(),
                request.getDate(),
                request.getTime()
        );
    }
}
