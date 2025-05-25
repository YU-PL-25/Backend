package PL_25.shuttleplay.Repository;

import PL_25.shuttleplay.Entity.Game.GameHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GameHistoryRepository extends JpaRepository<GameHistory, Long> {
    @Query("SELECT gh FROM GameHistory gh " +
       "JOIN gh.game g " +
       "JOIN g.participants p " +
       "WHERE p.user.userId = :userId")
    List<GameHistory> findByUserId(@Param("userId") Long userId);

}
