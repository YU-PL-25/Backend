package PL_25.shuttleplay.Entity.User;

import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Profile {
    @Id
    public Long profileId;

    private String nickname;    // 화면에 뜨는 사용자 닉네임
    private String gender;
    private int age;
    private String playStyle;   // 즐겜, 빡겜
    private String PreferredGameType;   // 선호 게임 타입
}
