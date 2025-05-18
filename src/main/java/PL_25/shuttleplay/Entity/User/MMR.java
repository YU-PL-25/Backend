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
    public long mmrId;

    private int rating; // mmr 점수
    private double winRate; // 사용자의 승률
    private int gamesPlayed; // 게임 횟수
    private int winsCount;  // 게임 횟수 중 승리 횟수
    private int tolerance = 200; // 허용 가능한 MMR 차이
}
