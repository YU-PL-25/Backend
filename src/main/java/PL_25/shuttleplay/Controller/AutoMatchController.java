package PL_25.shuttleplay.Controller;

import PL_25.shuttleplay.Dto.Matching.AutoMatchRequest;
import PL_25.shuttleplay.Entity.Game.Game;
import PL_25.shuttleplay.Entity.Game.GameRoom;
import PL_25.shuttleplay.Entity.Game.MatchQueueEntry;
import PL_25.shuttleplay.Entity.User.NormalUser;
import PL_25.shuttleplay.Repository.MatchQueueRepository;
import PL_25.shuttleplay.Repository.NormalUserRepository;
import PL_25.shuttleplay.Repository.GameRoomRepository;
import PL_25.shuttleplay.Service.AutoMatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalTime;

@RestController
@RequestMapping("/api/match/auto")
@RequiredArgsConstructor
public class AutoMatchController {

    private final AutoMatchService autoMatchService;
    private final MatchQueueRepository matchQueueRepository;
    private final NormalUserRepository normalUserRepository;
    private final GameRoomRepository gameRoomRepository;

    // 1. 매칭 큐 등록 (사전/현장 매칭 참여자 등록)
    @PostMapping("/queue")
    public MatchQueueEntry enqueue(@RequestParam Long userId,
                                   @RequestBody AutoMatchRequest request) {
        NormalUser user = normalUserRepository.findById(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자 없음"));

        MatchQueueEntry entry = new MatchQueueEntry();
        entry.setUser(user);
        entry.setProfile(request.getProfile());
        entry.setMmr(request.getMmr());
        entry.setLocation(request.getLocation());
        entry.setMatched(false);

        if (request.isPreMatch()) {
            entry.setDate(request.getDate());
            entry.setTime(request.getTime());
        } else {
            entry.setDate(LocalDate.now());
            entry.setTime(LocalTime.now());
        }

        // 게임방 설정 (선택적)
        if (user.getGameRoom() != null) {
            entry.setGameRoom(user.getGameRoom());
        }

        return matchQueueRepository.save(entry);
    }


    // 2. 사전매칭(구장 기준): 해당 게임방 내 유저 중 자동 매칭
    @PostMapping("/pre-court/{roomId}")
    public Game matchPreCourtInRoom(@PathVariable Long roomId) {
        Game game = autoMatchService.matchPreCourtFromRoom(roomId);
        if (game == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "매칭 조건에 맞는 사용자가 부족합니다.");
        }
        return game;
    }

    // 3. 현장매칭(구장 기준): 해당 게임방 내 유저 중 유사도 기반 자동 매칭
    @PostMapping("/live/{roomId}")
    public Game matchLiveInRoom(@PathVariable Long roomId) {
        Game game = autoMatchService.matchLiveCourtFromRoom(roomId);
        if (game == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "매칭 조건에 맞는 사용자가 부족합니다.");
        }
        return game;
    }

    // 4. 사전매칭(동네 기준): 거리 + 유사도 만족 시 게임방 생성
    @PostMapping("/pre-location")
    public GameRoom matchPreLocation() {
        GameRoom room = autoMatchService.createPreLocationMeetingRoomFromQueue();
        if (room == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "매칭 조건에 맞는 사용자가 부족합니다.");
        }
        return room;
    }
}
