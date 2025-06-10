package PL_25.shuttleplay.Entity.Game;

import PL_25.shuttleplay.Entity.User.NormalUser;
import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "GAME_PARTICIPANT")
@IdClass(GameParticipantId.class)
public class GameParticipant {

    @Id
    @ManyToOne
    @JoinColumn(name = "game_id")
    @JsonBackReference
    private Game game;

    @Id
    @ManyToOne
    @JoinColumn(name = "user_id")
    private NormalUser user;

    @Enumerated(EnumType.STRING)
    private TeamType team;

    public GameParticipant() {
    }

    public GameParticipant(NormalUser user, Game game) {
        this.user = user;
        this.game = game;
    }

    public Long getUserId() {
        return user != null ? user.getUserId() : null;
    }
}
