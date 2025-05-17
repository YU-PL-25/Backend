package PL_25.shuttleplay.Entity.User;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Entity
@RequiredArgsConstructor
public class Profile {
    @Id
    @GeneratedValue
    public Long profileId;

    private String ageGroup; // 연령대
    private String playStyle;   // 게임 성향: 즐겜, 빡겜
    private String gameType;   // 게임 타입: 단/복식

    public Profile(String ageGroup, String playStyle, String gametype) {
        this.ageGroup = ageGroup;
        this.playStyle = playStyle;
        this.gameType = gametype;
    }
}
