package PL_25.shuttleplay.Entity.User;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class MMR {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long mmrId;

    private int rating; // mmr 점수
    private double winRate; // 사용자의 승률
    private int gamesPlayed; // 게임 횟수?
    private int tolerance = 200; // 허용 가능한 MMR 차이

    // 사용자 게임 내역, 급수에 따라서 점수 계산하는 메소드
    public MMR calculatorMMR(Profile _profile) {
        return new MMR();
    }
}
