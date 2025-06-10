package PL_25.shuttleplay.Entity.Game;

import PL_25.shuttleplay.Entity.Location;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
public class Game {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long gameId;

    @ManyToOne
    @JoinColumn(name = "game_room_id")
    @JsonBackReference
    private GameRoom gameRoom;

    private boolean isPrematched;;

    // GameParticipant 엔티티 생성으로 매핑 수정
    @OneToMany(mappedBy = "game", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<GameParticipant> participants = new ArrayList<>();

    private LocalDate date;
    private LocalTime time;

    @ManyToOne(cascade = CascadeType.PERSIST)
    @JoinColumn(name = "location_id")
    private Location location;

    private String matchType;

    @OneToOne(mappedBy = "game", cascade = CascadeType.ALL)
    private GameHistory gameHistory;

    @Enumerated(EnumType.STRING)
    private GameStatus status;

}
