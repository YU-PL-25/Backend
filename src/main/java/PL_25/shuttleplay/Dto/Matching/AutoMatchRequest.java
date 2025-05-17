package PL_25.shuttleplay.Dto.Matching;

import PL_25.shuttleplay.Entity.Location;
import PL_25.shuttleplay.Entity.User.MMR;
import PL_25.shuttleplay.Entity.User.NormalUser;
import PL_25.shuttleplay.Entity.User.Profile;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

// 자동 매칭 요청 시 필요 입력값 객체

@Getter
@Setter
@RequiredArgsConstructor
public class AutoMatchRequest {

    private Profile profile;
    private MMR mmr;

    // 사전매칭용
    private LocalDate date;
    private LocalTime time;
    private Location location;

    private NormalUser user; // 동네 매칭 GameRoom 참여자 저장을 위한 필드

    public boolean isPreMatch(){
        return date != null && time != null;
    }

    public AutoMatchRequest(Profile profile, MMR mmr) {
        this.profile = profile;
        this.mmr = mmr;
    }

}
