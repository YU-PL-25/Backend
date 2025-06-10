package PL_25.shuttleplay.Repository;

import PL_25.shuttleplay.Entity.User.NormalUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NormalUserRepository extends JpaRepository<NormalUser, Long> {
    // 게임방에 들어온 사용자들 조회
    int countByGameRoom_GameRoomId(Long gameRoomId);

    NormalUser findFirstByEmail(String email);

}
