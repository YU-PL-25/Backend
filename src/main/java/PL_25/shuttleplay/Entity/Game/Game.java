package PL_25.shuttleplay.Entity.Game;

import PL_25.shuttleplay.Entity.User.User;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class Game {
    @Id
    public long gameId;

    private boolean isPrematched;
    private List<User> participants;
    private String matchType;
    private GameHistory gameHistory;
}
