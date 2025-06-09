package PL_25.shuttleplay.dto.Matching;

import PL_25.shuttleplay.Entity.Location;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CurrentMatchingGameRoomDTO {

    private Long masterId;
    private Location location;
    private String title;       // 게임방 제목

}
