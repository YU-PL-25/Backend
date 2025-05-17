package PL_25.shuttleplay.Entity.Game;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

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
