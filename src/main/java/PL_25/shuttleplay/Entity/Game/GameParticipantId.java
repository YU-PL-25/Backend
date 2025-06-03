package PL_25.shuttleplay.Entity.Game;

import java.io.Serializable;
import java.util.Objects;

public class GameParticipantId implements Serializable {
    private Long game;
    private Long user;

    public GameParticipantId() {}

    public GameParticipantId(Long gameId, Long userId) {
        this.game = gameId;
        this.user = userId;
    }
    public Long getGame() {
        return game;
    }

    public Long getUser() {
        return user;
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GameParticipantId)) return false;
        GameParticipantId that = (GameParticipantId) o;
        return Objects.equals(game, that.game) &&
                Objects.equals(user, that.user);
    }

    @Override
    public int hashCode() {
        return Objects.hash(game, user);
    }
}