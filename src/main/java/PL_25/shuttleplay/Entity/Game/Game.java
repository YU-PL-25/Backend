package PL_25.shuttleplay.Entity.Game;

import PL_25.shuttleplay.Entity.User.NormalUser;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
@Entity
public class Game {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long gameId;

    @ManyToOne
    @JoinTable(name = "game_room_id")
    private GameRoom gameRoomId;

    private boolean isPrematched;

    @ManyToMany
    @JoinTable(
            name = "game_participants",
            joinColumns = @JoinColumn(name = "game_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private List<NormalUser> participants;

    private String matchType;

    @OneToOne(mappedBy = "game", cascade = CascadeType.ALL)
    private GameHistory gameHistory;
}
