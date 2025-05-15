package PL_25.shuttleplay.Entity.User;

import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MMR {
    @Id
    public long mmrId;

    private int rating; // mmr 점수
    private double winRate; // 사용자의 승률
    private int gamesPlayed;    // 게임 횟수?

    // 사용자 게임 내역, 급수에 따라서 점수 계산하는 메소드
    public MMR calculatorMMR(Profile _profile) {
        return new MMR();
    }
}
