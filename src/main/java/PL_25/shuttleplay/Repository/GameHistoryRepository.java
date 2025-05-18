package PL_25.shuttleplay.Repository;

import PL_25.shuttleplay.Entity.Game.GameHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GameHistoryRepository extends JpaRepository<GameHistory, Long> {
}
