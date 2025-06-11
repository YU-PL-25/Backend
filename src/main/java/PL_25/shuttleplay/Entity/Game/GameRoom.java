package PL_25.shuttleplay.Entity.Game;

import PL_25.shuttleplay.Entity.Location;
import PL_25.shuttleplay.Entity.User.NormalUser;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

// 게임방(=대기방) 을 의미하는 클래스, 이 안에 Game 객체가 여러개 들어간다. Game은 경기 1개에 해당

@Getter
@Setter
@Entity
@Builder
@RequiredArgsConstructor
@AllArgsConstructor
public class GameRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long gameRoomId;

    @OneToMany(mappedBy = "gameRoom")
    @JsonManagedReference
    private List<NormalUser> participants;

    @OneToMany(mappedBy = "gameRoom", cascade = CascadeType.ALL)
    @JsonManagedReference("gameRoom-gameList")
    private List<Game> gameList = new ArrayList<>();

    @ManyToOne(cascade = CascadeType.PERSIST)
    @JoinColumn(name = "location_id")
    private Location location;

    private String title;


    private LocalDate date;
    private LocalTime time;

    // 방 생성자 (방장, manager)
    @ManyToOne
    @JoinColumn(name = "created_by")
    private NormalUser createdBy;

//    // 사용자의 프로필 정보를 입력받아, 해당 게임방에 있는 사람들과 경기를 매칭해줌
//    public Game startMatching(Profile userProfile) {
//        Game newGame = new Game();
//        return newGame;
//    }
}
