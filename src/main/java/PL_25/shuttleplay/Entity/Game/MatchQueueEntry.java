package PL_25.shuttleplay.Entity.Game;

import PL_25.shuttleplay.Entity.Location;
import PL_25.shuttleplay.Entity.User.MMR;
import PL_25.shuttleplay.Entity.User.NormalUser;
import PL_25.shuttleplay.Entity.User.Profile;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class MatchQueueEntry{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private NormalUser user;

    @ManyToOne(cascade = CascadeType.PERSIST)
    private Profile profile;

    @ManyToOne(cascade = CascadeType.PERSIST)
    private MMR mmr;

    @ManyToOne(cascade = CascadeType.PERSIST)
    @JoinColumn(name = "location_id")
    private Location location;

    private LocalDate date;
    private LocalTime time;

    private boolean matched;

    @ManyToOne
    @JoinColumn(name = "game_room_id")
    private GameRoom gameRoom;

    @Column(name = "is_prematched")
    private Boolean isPrematched = false;

    @Enumerated(EnumType.STRING)
    private MatchQueueType matchType;

    // 자동 게임방 매칭 인원
    private int requiredMatchCount;

    public Long getUserId() {
        return user != null ? user.getUserId() : null;
    }
}
