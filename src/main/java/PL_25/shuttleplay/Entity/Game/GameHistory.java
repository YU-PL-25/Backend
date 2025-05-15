package PL_25.shuttleplay.Entity.Game;

import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class GameHistory {
    @Id
    public Game gameHistoryId;

    @ManyToOne
    @JoinColumn(name = "gameId", nullable = false)
    private Game gameId;

    private LocalDate date;
    private int score;
    private boolean isWin;
}
