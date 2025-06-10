package PL_25.shuttleplay.dto.Matching;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
public class GameDTO {
    private Long gameId;
    private String matchType;
    private String status;
    private String date;
    private String time;
    private List<PlayerDTO> players;    // 해당 game에 참가중인 참가자 정보
}
