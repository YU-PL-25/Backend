package PL_25.shuttleplay.dto;

import PL_25.shuttleplay.Entity.Game.GameHistory;
import lombok.Getter;
import lombok.Setter;

/*********************************************
* 사용자가 게임 종료 후 스코어를 입력받아서 전달하는 DTO
* GameHistory 객체를 생성하기 위한 입력 과정
* ********************************************/

@Getter
@Setter
public class GameHistoryDTO {
//    private Long gameId;      // gameId는 자동으로 조회해서 불러와서 사용해야 함

    // 경기 종료 시 사용자가 입력해줘야 하는 '팀 정보'
    private Long userId;
    private Long opponentId;
    // 경기 종료 시 사용자가 입력해줘야 하는 '경기 결과'
    private int scoreTeamA;
    private int scoreTeamB;
    private boolean isCompleted;

    public GameHistory toGameHistory() {
        GameHistory gh = new GameHistory();
        gh.setScoreTeamA(scoreTeamA);
        gh.setScoreTeamB(scoreTeamB);
        gh.setCompleted(isCompleted);
        return gh;
    }
}
