package PL_25.shuttleplay.Controller;

import PL_25.shuttleplay.Entity.Game.GameRoom;
import PL_25.shuttleplay.Entity.Location;
import PL_25.shuttleplay.Entity.User.MMR;
import PL_25.shuttleplay.Entity.User.NormalUser;
import PL_25.shuttleplay.Entity.User.Profile;
import PL_25.shuttleplay.Repository.GameRoomRepository;
import PL_25.shuttleplay.Repository.NormalUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SetupController {

    private final GameRoomRepository gameRoomRepository;
    private final NormalUserRepository normalUserRepository;

    // 게임방 생성
    @PostMapping("/room/create")
    public GameRoom createRoom(@RequestBody Map<String, String> req) {
        GameRoom room = new GameRoom();
        Location loc = new Location();
        loc.setCourtName(req.get("courtName"));
        loc.setCourtAddress(req.get("courtAddress"));
        room.setLocation(loc);
        room.setDate(LocalDate.parse(req.get("date")));
        room.setTime(LocalTime.parse(req.get("time")));
        return gameRoomRepository.save(room);
    }

    // 유저 생성 + 게임방 연결
    @PostMapping("/user/create")
    public NormalUser createUser(@RequestBody Map<String, Object> req) {
        NormalUser user = new NormalUser();
        user.setNickname(req.get("nickname").toString());
        user.setGender(req.get("gender").toString());
        user.setEmail(req.get("email").toString());
        user.setPhone(req.get("phone").toString());
        user.setRank(req.get("rank").toString());
        user.setRole("normal");

        // MMR 입력
        Map<String, Object> mmrMap = (Map<String, Object>) req.get("mmr");
        MMR mmr = new MMR();
        mmr.setRating(Integer.parseInt(mmrMap.get("rating").toString()));
        mmr.setTolerance(Integer.parseInt(mmrMap.get("tolerance").toString()));
        user.setMmr(mmr);

        // Profile 입력
        Map<String, Object> profileMap = (Map<String, Object>) req.get("profile");
        Profile profile = new Profile();
        profile.setAgeGroup(profileMap.get("ageGroup").toString());
        profile.setGameType(profileMap.get("gameType").toString());
        profile.setPlayStyle(profileMap.get("playStyle").toString());
        user.setProfile(profile);

        // 게임방 연결은 선택
        if (req.containsKey("gameRoomId")) {
            Long roomId = Long.parseLong(req.get("gameRoomId").toString());
            GameRoom room = gameRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("해당 게임방 없음"));
            user.setGameRoom(room);
        }

        return normalUserRepository.save(user);
    }
}

