package PL_25.shuttleplay.Controller;

import PL_25.shuttleplay.Dto.Matching.AutoMatchRequest;
import PL_25.shuttleplay.Entity.Game.GameRoom;
import PL_25.shuttleplay.Entity.User.NormalUser;
import PL_25.shuttleplay.Repository.GameRoomRepository;
import PL_25.shuttleplay.Repository.NormalUserRepository;
import PL_25.shuttleplay.Service.AutoMatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/match/auto")
public class AutoMatchController {
    private final AutoMatchService autoMatchService;
    private final GameRoomRepository gameRoomRepository;
    private final NormalUserRepository normalUserRepository;

    // 사전 매칭(구장 기준) - 게임방 내 유저 중에서 자동 매칭
    @PostMapping("/pre-court/{roomId}")
    public List<AutoMatchRequest> matchPreCourt(@PathVariable Long roomId, @RequestBody AutoMatchRequest me) {
        GameRoom room = gameRoomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("해당 게임방이 존재하지 않습니다."));

        List<AutoMatchRequest> pool = room.getParticipants().stream()
                .filter(user -> !user.equals(me.getUser()))
                .filter(user -> !user.isInGame())
                .map(user -> {
                    AutoMatchRequest request = new AutoMatchRequest(user.getProfile(), user.getMmr());
                    request.setUser(user);
                    request.setDate(room.getDate());
                    request.setTime(room.getTime());
                    request.setLocation(room.getLocation());
                    return request;
                })
                .toList();

        return autoMatchService.matchLiveCourt(me, pool);
    }

    // 현장 매칭 (구장 기준) - 게임방 내 유저 중에서 자동 매칭
    @PostMapping("/live/{roomId}")
    public List<AutoMatchRequest> matchLive(@PathVariable Long roomId, @RequestBody AutoMatchRequest me) {
        GameRoom room = gameRoomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("해당 게임방이 존재하지 않습니다"));

        List<AutoMatchRequest> pool = room.getParticipants().stream()
                .filter(user -> !user.equals(me.getUser()))
                .filter(user -> !user.isInGame())
                .map(user -> {
                    AutoMatchRequest request = new AutoMatchRequest(user.getProfile(), user.getMmr());
                    request.setUser(user);
                    return request;
                })
                .toList();
        return autoMatchService.matchLiveCourt(me, pool);
    }

    // 사전 매칭 (동네 기준) - 전체 사용자 중 거리+유사도 만족 → 게임방 생성
    @PostMapping("/pre-location")
    public GameRoom matchPreLocationAndCreateRoom(@RequestBody AutoMatchRequest me){
        List<NormalUser> allUsers = normalUserRepository.findAll();

        List<AutoMatchRequest> pool = allUsers.stream()
                .filter(user -> !user.equals(me.getUser()))
                .filter(user -> !user.isInGame())
                .map(user -> {
                    AutoMatchRequest request = new AutoMatchRequest(user.getProfile(), user.getMmr());
                    request.setUser(user);
                    request.setDate(me.getDate());
                    request.setTime(me.getTime());
                    request.setLocation(me.getLocation());
                    return request;
                })
                .toList();
        return autoMatchService.createPreLocationMeetingRoom(me, pool);
    }
}