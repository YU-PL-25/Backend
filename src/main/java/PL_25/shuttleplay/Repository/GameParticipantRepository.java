package PL_25.shuttleplay.Repository;

import PL_25.shuttleplay.Entity.Game.*;
import PL_25.shuttleplay.Entity.User.NormalUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GameParticipantRepository extends JpaRepository<GameParticipant, GameParticipantId> {
    Optional<GameParticipant> findById(GameParticipantId id);
    List<GameParticipant> findByGame(Game game);
    Optional<GameParticipant> findByGameAndUser(Game game, NormalUser user);
    List<GameParticipant> findByGameAndTeam(Game game, TeamType team);
    Optional<GameParticipant> findByGame_GameIdAndUser_UserId(Long gameId, Long userId);
    boolean existsByUser_UserIdAndGame_Status(Long userId, GameStatus status);
    List<GameParticipant> findAllByGame_GameId(Long gameId);
    List<GameParticipant> findByUser_UserId(Long userId);
}