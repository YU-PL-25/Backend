package PL_25.shuttleplay.Repository;

import PL_25.shuttleplay.Entity.Game.Game;
import PL_25.shuttleplay.Entity.Game.GameStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GameRepository extends JpaRepository<Game, Long> {
    // 특정 게임방에서 진행된 경기 목록 조회
    List<Game> findByGameRoom_GameRoomId(Long gameRoomId);

//    boolean existsByParticipants_UserIdAndStatus(Long userId, GameStatus status);     // GameParticipantRepo로 이동
}
