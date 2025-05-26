package PL_25.shuttleplay.Service;

import PL_25.shuttleplay.Entity.Game.Game;
import PL_25.shuttleplay.Entity.Game.GameHistory;
import PL_25.shuttleplay.Entity.Game.GameParticipant;
import PL_25.shuttleplay.Entity.Game.TeamType;
import PL_25.shuttleplay.Entity.User.NormalUser;
import PL_25.shuttleplay.Repository.GameParticipantRepository;
import PL_25.shuttleplay.Repository.GameRepository;
import PL_25.shuttleplay.Repository.NormalUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GameService {

    private final GameRepository gameRepository;
    private final GameParticipantRepository gameParticipantRepository;
    private final NormalUserRepository normalUserRepository;

    // 참가자의 팀 지정
    public void assignTeam(Long gameId, Long userId, String team) {
        GameParticipant participant = gameParticipantRepository.findByGame_GameIdAndUser_UserId(gameId, userId)
                .orElseThrow(() -> new IllegalArgumentException("해당 참가자가 없습니다."));
        participant.setTeam(TeamType.valueOf(team.toUpperCase()));
        gameParticipantRepository.save(participant);
    }

    // 경기 종료 시 팀 구분해서 MMR 계산 준비 -> 이후 MMRService 에서 계산 로직 실행
    public void processGameResult(Game game, GameHistory gameHistory) {
        List<GameParticipant> teamA = gameParticipantRepository.findByGameAndTeam(game, TeamType.TEAM_A);
        List<GameParticipant> teamB = gameParticipantRepository.findByGameAndTeam(game, TeamType.TEAM_B);

        List<NormalUser> teamAUsers = teamA.stream().map(GameParticipant::getUser).toList();
        List<NormalUser> teamBUsers = teamB.stream().map(GameParticipant::getUser).toList();
    }
}