package PL_25.shuttleplay.Entity.Game;

import PL_25.shuttleplay.Entity.User.NormalUser;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@IdClass(GameParticipantId.class)
public class GameParticipant {

    @Id
    @ManyToOne
    @JoinColumn(name = "game_id")
    private Game game;

    @Id
    @ManyToOne
    @JoinColumn(name = "user_id")
    private NormalUser user;

    @Enumerated(EnumType.STRING)
    private TeamType team;

    public Long getUserId() {
        return user != null ? user.getUserId() : null;
    }
}
