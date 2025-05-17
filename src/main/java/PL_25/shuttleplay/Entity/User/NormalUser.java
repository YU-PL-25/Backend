package PL_25.shuttleplay.Entity.User;

import PL_25.shuttleplay.Entity.Game.Game;
import PL_25.shuttleplay.Entity.Game.GameRoom;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Entity
public class NormalUser extends User {
//    @Id
//    public long userId;

    private String name;
    private String nickname; // 사용자 닉네임
    private String gender; // 성별
    private String email;
    private String phone;
    private String rank;

    @OneToOne(cascade = CascadeType.ALL)
    private MMR mmr;    // mmr 점수

    @OneToOne(cascade = CascadeType.ALL)
    private Profile profile;    // 개인 게임 정보를 담은 프로필 객체

    @ManyToOne
    @JoinColumn(name = "game_room_id")
    private GameRoom gameRoom; // 참여 게임방

    private String role;    // admin(?? 관리자), normal(일반 사용자), manager(방 관리자)

    // 현재 참여중인 게임
    @ManyToOne
    @JoinColumn(name = "game_id")
    private Game currentGame;

    // 게임에 참여 중인지 확인
    public boolean isInGame(){
        return currentGame != null;
    }
}
