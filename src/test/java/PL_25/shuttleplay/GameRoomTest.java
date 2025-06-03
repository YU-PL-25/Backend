package PL_25.shuttleplay;

import PL_25.shuttleplay.Entity.Game.GameRoom;
import PL_25.shuttleplay.Entity.Location;
import PL_25.shuttleplay.Entity.User.NormalUser;
import PL_25.shuttleplay.Entity.User.Rank;
import PL_25.shuttleplay.Repository.GameRoomRepository;
import PL_25.shuttleplay.Repository.LocationRepository;
import PL_25.shuttleplay.Repository.NormalUserRepository;
import PL_25.shuttleplay.Service.NormalUserService;
import PL_25.shuttleplay.dto.UserRegisterDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.annotation.Rollback;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@SpringBootTest
public class GameRoomTest {

    @Autowired
    private GameRoomRepository gameRoomRepository;
    @Autowired
    private NormalUserRepository normalUserRepository;
    @Autowired
    private LocationRepository locationRepository;
    @Autowired
    private NormalUserService normalUserService;

    @Test
    public void insertMockUser() {

        try {
            NormalUser normalUser = new NormalUser();
            normalUser.setRole("normal");
            normalUser.setEmail("33@gmail.com");
            normalUser.setGender("남자");
            normalUser.setName("이름3");
            normalUser.setNickname("닉네임3");
            normalUser.setPhone("010-0000-0000");
            normalUser.setRank(Rank.A);

            normalUserRepository.save(normalUser);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    @Test
    @Transactional
    @Rollback(false)
    public void insertGameRoom() {
        try {

            Location location = locationRepository.findById(1L)
                    .orElseThrow(() -> new IllegalArgumentException("Location not found"));


            GameRoom gameRoom = new GameRoom();
            gameRoom.setLocation(location);
            gameRoom.setDate(LocalDate.now());
            gameRoom.setTime(LocalTime.now());


            GameRoom savedGameRoom = gameRoomRepository.save(gameRoom);

            assertNotNull(savedGameRoom);
            assertNotNull(savedGameRoom.getGameRoomId());

        } catch (Exception e) {
            e.printStackTrace();  // 상세 에러 로그 출력
            fail("게임방 생성 실패: " + e.getMessage());
        }
    }


    @Test
    @Transactional
    @Rollback(value = false)
    public void selectGameRoomParticipants() {

        GameRoom gameRoom = gameRoomRepository.findById(53L).orElseThrow();
        List<NormalUser> participants = gameRoom.getParticipants();
        for (NormalUser n : participants) {
            System.out.println(n.getUserId());
        }
    }


    @Test
    @Transactional
    @Rollback(false)
    public void insertNormalUserTest() {

        UserRegisterDTO userRegisterDTO = new UserRegisterDTO();
        userRegisterDTO.setLoginId("abc123");
        userRegisterDTO.setPassword("aaa111");
        userRegisterDTO.setRankStr("A");

        normalUserService.createUser(userRegisterDTO);

    }
}


