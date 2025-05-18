package PL_25.shuttleplay.Service;

import PL_25.shuttleplay.Entity.Game.GameHistory;
import PL_25.shuttleplay.Entity.User.MMR;
import PL_25.shuttleplay.Entity.User.NormalUser;
import PL_25.shuttleplay.Entity.User.Rank;
import org.springframework.stereotype.Service;

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

    public void updateMmr(NormalUser user, NormalUser opponent, GameHistory gameHistory) {
        MMR userMMR = user.getMmr();
        MMR oppMMR = opponent.getMmr();

        boolean didWin = gameHistory.getScoreTeamA() > gameHistory.getScoreTeamB();

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
}
