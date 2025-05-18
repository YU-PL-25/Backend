package PL_25.shuttleplay;

import PL_25.shuttleplay.Dto.Matching.AutoMatchRequest;
import PL_25.shuttleplay.Entity.Game.Game;
import PL_25.shuttleplay.Entity.Game.GameRoom;
import PL_25.shuttleplay.Entity.Location;
import PL_25.shuttleplay.Entity.User.MMR;
import PL_25.shuttleplay.Entity.User.NormalUser;
import PL_25.shuttleplay.Entity.User.Profile;
import PL_25.shuttleplay.Service.AutoMatchService;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class AutoMatchingTest {

    @Test
    public void testPreCourtAutoMatchingOnlyFromRoomAndNotInGame() {
        // 1. 게임방 설정
        GameRoom room = new GameRoom();
        room.setGameRoomId(1L);

        Location location = new Location();
        location.setCourtName("서울체육관");
        location.setCourtAddress("서울 송파구 올림픽로 25");
        room.setLocation(location);
        room.setDate(LocalDate.of(2025, 5, 25));
        room.setTime(LocalTime.of(18, 0));

        // 2. 기준 유저 me
        MMR mmrMe = new MMR();
        mmrMe.setRating(1450);

        NormalUser me = new NormalUser();
        me.setNickname("나");
        me.setProfile(new Profile("20대", "혼합복식", "즐겜"));
        me.setMmr(mmrMe);
        me.setCurrentGame(null); // 게임 미참여
        me.setGameRoom(room);

        AutoMatchRequest meRequest = new AutoMatchRequest(me.getProfile(), me.getMmr());
        meRequest.setUser(me);
        meRequest.setDate(room.getDate());
        meRequest.setTime(room.getTime());
        meRequest.setLocation(location);

        // 3. 게임방 내 유저 리스트
        List<NormalUser> users = new ArrayList<>();

        // ✅ 유저1: 유사도 높고 게임 미참여 → 매칭됨
        MMR mmr1 = new MMR(); mmr1.setRating(1470);
        NormalUser u1 = new NormalUser();
        u1.setNickname("유저1");
        u1.setProfile(new Profile("20대", "혼합복식", "즐겜"));
        u1.setMmr(mmr1);
        u1.setCurrentGame(null);
        u1.setGameRoom(room);
        users.add(u1);

        // ✅ 유저2: 유사도 낮음 → 매칭 제외
        MMR mmr2 = new MMR(); mmr2.setRating(1700);
        NormalUser u2 = new NormalUser();
        u2.setNickname("유저2");
        u2.setProfile(new Profile("30대", "남자복식", "빡겜"));
        u2.setMmr(mmr2);
        u2.setCurrentGame(null);
        u2.setGameRoom(room);
        users.add(u2);

        // ✅ 유저3: 유사도 높지만 게임 참여 중 → 매칭 제외
        MMR mmr3 = new MMR(); mmr3.setRating(1440);
        NormalUser u3 = new NormalUser();
        u3.setNickname("유저3");
        u3.setProfile(new Profile("20대", "혼합복식", "즐겜"));
        u3.setMmr(mmr3);
        u3.setCurrentGame(new Game()); // 가짜 참여 중
        u3.setGameRoom(room);
        users.add(u3);

        // 4. AutoMatchRequest 변환
        List<AutoMatchRequest> pool = users.stream()
                .filter(u -> !u.equals(me))
                .filter(u -> u.getCurrentGame() == null)
                .map(u -> {
                    AutoMatchRequest req = new AutoMatchRequest(u.getProfile(), u.getMmr());
                    req.setUser(u);
                    req.setDate(room.getDate());
                    req.setTime(room.getTime());
                    req.setLocation(room.getLocation());
                    return req;
                }).toList();

        // 5. 매칭 실행
        AutoMatchService service = new AutoMatchService(); // 의존성 없는 상태로 사용
        List<AutoMatchRequest> result = service.matchPreCourt(meRequest, pool);

        // 6. 검증
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUser().getNickname()).isEqualTo("유저1");
    }
}
