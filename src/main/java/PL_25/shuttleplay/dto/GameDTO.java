package PL_25.shuttleplay.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class GameDTO {
    private Long gameId;
    private String matchType;
    private String status;
    private String date;
    private String time;
}
