//package PL_25.shuttleplay.Controller;
//
//import PL_25.shuttleplay.Entity.Game.GameRoom;
//import PL_25.shuttleplay.Entity.Location;
//import PL_25.shuttleplay.Entity.User.MMR;
//import PL_25.shuttleplay.Entity.User.NormalUser;
//import PL_25.shuttleplay.Entity.User.Profile;
//import PL_25.shuttleplay.Repository.GameRoomRepository;
//import PL_25.shuttleplay.Repository.NormalUserRepository;
//import lombok.RequiredArgsConstructor;
//import org.springframework.http.HttpStatus;
//import org.springframework.web.bind.annotation.PostMapping;
//import org.springframework.web.bind.annotation.RequestBody;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RestController;
//import org.springframework.web.server.ResponseStatusException;
//
//import java.time.LocalDate;
//import java.time.LocalTime;
//import java.util.Map;
//
//@RestController
//@RequestMapping("/api")
//@RequiredArgsConstructor
//public class SetupController {
//
//    private final GameRoomRepository gameRoomRepository;
//    private final NormalUserRepository normalUserRepository;
//
//    // 게임방 생성
//    @PostMapping("/room/create")
//    public GameRoom createRoom(@RequestBody Map<String, String> req) {
//        GameRoom room = new GameRoom();
//        Location loc = new Location();
//        loc.setCourtName(req.get("courtName"));
//        loc.setCourtAddress(req.get("courtAddress"));
//        room.setLocation(loc);
//        room.setDate(LocalDate.parse(req.get("date")));
//        room.setTime(LocalTime.parse(req.get("time")));
//        return gameRoomRepository.save(room);
//    }
//
//    // 유저 생성 + 게임방 연결
//    @PostMapping("/user/create")
//    public NormalUser createUser(@RequestBody Map<String, Object> req) {
//    // 필수값 검증
//    if (req.get("nickname") == null || req.get("mmr") == null || req.get("profile") == null) {
//        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "필수 필드가 누락되었습니다.");
//    }
//
//    NormalUser user = new NormalUser();
//    user.setNickname(req.get("nickname").toString());
//    user.setGender(req.get("gender").toString());
//    user.setEmail(req.get("email").toString());
//    user.setPhone(req.get("phone").toString());
//    user.setRank(req.get("rank").toString());
//    user.setRole("normal");
//
//    // MMR
//    Map<String, Object> mmrMap = (Map<String, Object>) req.get("mmr");
//    if (mmrMap.get("rating") == null || mmrMap.get("tolerance") == null) {
//        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "MMR 필드가 누락되었습니다.");
//    }
//
//    MMR mmr = new MMR();
//    mmr.setRating(Integer.parseInt(mmrMap.get("rating").toString()));
//    mmr.setTolerance(Integer.parseInt(mmrMap.get("tolerance").toString()));
//    user.setMmr(mmr);
//
//    // Profile
//    Map<String, Object> profileMap = (Map<String, Object>) req.get("profile");
//    if (profileMap.get("ageGroup") == null || profileMap.get("gameType") == null || profileMap.get("playStyle") == null) {
//        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "프로필 필드가 누락되었습니다.");
//    }
//
//    Profile profile = new Profile();
//    profile.setAgeGroup(profileMap.get("ageGroup").toString());
//    profile.setGameType(profileMap.get("gameType").toString());
//    profile.setPlayStyle(profileMap.get("playStyle").toString());
//    user.setProfile(profile);
//
//    // 선택적 게임방 연결
//    if (req.containsKey("gameRoomId")) {
//        Long roomId = Long.parseLong(req.get("gameRoomId").toString());
//        GameRoom room = gameRoomRepository.findById(roomId)
//            .orElseThrow(() -> new IllegalArgumentException("해당 게임방 없음"));
//        user.setGameRoom(room);
//    }
//
//    return normalUserRepository.save(user);
//}
//
//}
//
