package PL_25.shuttleplay.Entity.Game;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**********************************
* 각각의 경기에 대한 결과 정보를 담는 클래스
* - 팀A, B의 점수
* - 경기 완료 여부
* *********************************/

@Getter
@Setter
@Entity
public class GameHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long gameHistoryId;

    @OneToOne
    @JoinColumn(name = "game_id")
    private Game game;

    private int scoreTeamA;
    private int scoreTeamB;
    private boolean isCompleted;

}
