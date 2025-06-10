package PL_25.shuttleplay.dto.Matching;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PlayerDTO {
    private Long userId;
    private String nickname;
    private String rank;
    private String team;
}
