package PL_25.shuttleplay.Service;

import PL_25.shuttleplay.Entity.Game.Game;
import PL_25.shuttleplay.Entity.Game.GameHistory;
import PL_25.shuttleplay.Entity.Game.GameParticipant;
import PL_25.shuttleplay.Entity.Game.TeamType;
import PL_25.shuttleplay.Entity.User.MMR;
import PL_25.shuttleplay.Entity.User.NormalUser;
import PL_25.shuttleplay.Entity.User.Rank;
import org.springframework.stereotype.Service;

import java.util.List;

/**********************************
* MMR 점수와 관련한 계산 로직을 처리하는 곳
* *********************************/
@Service
public class MMRService {
    public MMR createInitialMmr(Rank rank) {
        MMR mmr = new MMR();
        mmr.setRating(rank.getInitialMmr());
        mmr.setGamesPlayed(0);
        mmr.setWinRate(0);
        mmr.setWinsCount(0);
        return mmr;
    }

    // 개인, 단식 MMR 계산
    public void updateMmr(NormalUser user, NormalUser opponent, GameHistory gameHistory) {
        MMR userMMR = user.getMmr();
        MMR oppMMR = opponent.getMmr();
        // ✅ user가 A팀인지 B팀인지 판단
        boolean userIsTeamA = gameHistory.getGame().getParticipants().stream()
                .filter(p -> p.getUser().getUserId().equals(user.getUserId()))
                .findFirst()
                .map(p -> p.getTeam() == TeamType.TEAM_A)
                .orElseThrow(() -> new IllegalArgumentException("유저의 팀 정보를 찾을 수 없습니다."));

        boolean didWin = (userIsTeamA && gameHistory.getScoreTeamA() > gameHistory.getScoreTeamB()) ||
                (!userIsTeamA && gameHistory.getScoreTeamB() > gameHistory.getScoreTeamA());

        int userRating = userMMR.getRating();
        int oppRating = oppMMR.getRating();

        double expectedScore = 1 / (1 + Math.pow(10, (oppRating - userRating) / 400.0));
        double k = 30.0;
        double delta = didWin ? k * (1 - expectedScore) : -k * expectedScore;

        int newRating = (int) Math.round(userRating + delta);
        userMMR.setRating(newRating);

        userMMR.setGamesPlayed(userMMR.getGamesPlayed() + 1);
        if (didWin) userMMR.setWinsCount(userMMR.getWinsCount() + 1);

        double newWinRate = (double) userMMR.getWinsCount() / userMMR.getGamesPlayed();
        userMMR.setWinRate(newWinRate);

    }

    // 복식 계산 시 각 팀원에게 적용할 메소드 (오버로딩)
    public void updateMmr(NormalUser user, double opponentAvgMmr, boolean didWin) {
        MMR userMMR = user.getMmr();

        double expectedScore = 1 / (1 + Math.pow(10, (opponentAvgMmr - userMMR.getRating()) / 400.0));
        double k = 30.0;
        double delta = didWin ? k * (1 - expectedScore) : -k * expectedScore;

        int newRating = (int) Math.round(userMMR.getRating() + delta);
        userMMR.setRating(newRating);

        userMMR.setGamesPlayed(userMMR.getGamesPlayed() + 1);
        if (didWin) userMMR.setWinsCount(userMMR.getWinsCount() + 1);

        double newWinRate = (double) userMMR.getWinsCount() / userMMR.getGamesPlayed();
        userMMR.setWinRate(newWinRate);
    }

    // 복식 MMR 계산 (각 팀의 평균 mmr 점수를 기준으로 계산)
    public void updateMmrForTeamMatch(Game game, GameHistory history) {
        List<GameParticipant> participants = game.getParticipants();

        // A팀, B팀 분리
        List<NormalUser> teamA = participants.stream()
                .filter(p -> p.getTeam() == TeamType.TEAM_A)
                .map(GameParticipant::getUser)
                .toList();

        List<NormalUser> teamB = participants.stream()
                .filter(p -> p.getTeam() == TeamType.TEAM_B)
                .map(GameParticipant::getUser)
                .toList();

        // 팀 평균 MMR 계산
        double avgA = teamA.stream().mapToInt(u -> u.getMmr().getRating()).average().orElse(1500);
        double avgB = teamB.stream().mapToInt(u -> u.getMmr().getRating()).average().orElse(1500);

        // 승리 여부 판단
        boolean teamAWon = history.getScoreTeamA() > history.getScoreTeamB();

        for (NormalUser user : teamA) {
            updateMmr(user, avgB, teamAWon);
        }
        for (NormalUser user : teamB) {
            updateMmr(user, avgA, !teamAWon);
        }
    }
}
