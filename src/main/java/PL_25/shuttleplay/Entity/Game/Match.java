package PL_25.shuttleplay.Entity.Game;

import PL_25.shuttleplay.Entity.Location;
import PL_25.shuttleplay.Entity.User.Profile;
import PL_25.shuttleplay.Entity.User.User;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

// 게임방(=대기방) 을 의미하는 클래스, 이 안에 Game 객체가 여러개 들어간다. Game은 경기 1개에 해당

@Getter
@Setter
public class Match {
    @Id
    public long matchId;

    private List<User> participants;
    private Location location;
    private LocalDate date;
    private List<Game> gameList;

    // 사용자의 프로필 정보를 입력받아, 해당 게임방에 있는 사람들과 경기를 매칭해줌
    public Game startMatching(Profile userProfile) {
        Game newGame = new Game();
        return newGame;
    }
}
