package PL_25.shuttleplay.dto.Matching;

import PL_25.shuttleplay.Entity.Game.TeamType;
import lombok.Getter;
import lombok.Setter;

/*************************************************
* Game이 매칭된 뒤, TEAM A, B를 구분하기 위한 입력을 받아옴
* ************************************************/
@Getter
@Setter
public class TeamAssignmentDTO {
    private Long userId;
    private String team; // "A" or "B"

    // 입력받은 string 타입의 팀 구분을 -> TeamType으로 변환
    public TeamType getParsedTeamType() {
        try {
            return TeamType.valueOf(team.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("지원하지 않는 팀 타입입니다." + team);
        }
    }
}
