package PL_25.shuttleplay.Service;

import PL_25.shuttleplay.Entity.Game.Game;
import PL_25.shuttleplay.Entity.Game.GameParticipant;
import PL_25.shuttleplay.Entity.Game.GameStatus;
import PL_25.shuttleplay.Entity.Game.TeamType;
import PL_25.shuttleplay.Entity.User.NormalUser;
import PL_25.shuttleplay.Repository.GameParticipantRepository;
import PL_25.shuttleplay.Repository.GameRepository;
import PL_25.shuttleplay.Repository.NormalUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class GameParticipantService {

    private final GameParticipantRepository gameParticipantRepository;
    private final GameRepository gameRepository;
    private final NormalUserRepository normalUserRepository;

    public List<GameParticipant> findByGame(Game game) {
        return gameParticipantRepository.findByGame(game);
    }

    public void saveAll(List<GameParticipant> participants) {
        gameParticipantRepository.saveAll(participants);
    }

    // 팀 구분
    public void assignTeam(Long gameId, Long userId, TeamType teamType) {
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new IllegalArgumentException("해당 게임이 존재하지 않음"));
        NormalUser user = normalUserRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("해당 유저가 존재하지 않음"));

        GameParticipant participant = gameParticipantRepository
                .findByGameAndUser(game, user)
                .orElseThrow(() -> new IllegalArgumentException("해당 참가자가 존재하지 않음"));

        participant.setTeam(teamType);
        gameParticipantRepository.save(participant);
    }

    // 경기 결과 입력 시 해당 사용자가 참여중인 GameId 찾기
    public Optional<Game> findFinishedGameByUser(Long userId) {
        return gameParticipantRepository.findByUser_UserId(userId).stream()
                .map(GameParticipant::getGame)
                .filter(game -> game.getStatus() == GameStatus.FINISHED)
                .findFirst();
    }
}
