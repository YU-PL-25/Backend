package PL_25.shuttleplay.Controller;

import PL_25.shuttleplay.Entity.Game.GameHistory;
import PL_25.shuttleplay.Repository.GameHistoryRepository;
import PL_25.shuttleplay.Service.NormalUserService;
import PL_25.shuttleplay.dto.GameHistoryDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/*********************************************
* GameRoom 내의 각 Game 에 대한 컨트롤러
* - 게임 종료 시 결과 입력하여 GameHistory 객체 생성
* - ...
* *******************************************/

@RestController
@RequestMapping("/api/game")
@RequiredArgsConstructor
public class GameController {
    private final NormalUserService normalUserService;
    private final GameHistoryRepository gameHistoryRepository;

    // 경기 결과 입력 및 MMR 점수 갱신
    @PostMapping("/result")
    public ResponseEntity<String> inputGameResult(@RequestBody GameHistoryDTO dto) {
        // dto로부터 입력받은 정보를 가지고 GameHistory 객체 생성
        GameHistory gameHistory = new GameHistory();
        gameHistory.setScoreTeamA(dto.getScoreTeamA());
        gameHistory.setScoreTeamB(dto.getScoreTeamB());
        gameHistory.setCompleted(dto.isCompleted());
        // GameHistory 먼저 저장
        gameHistoryRepository.save(gameHistory);


        // MMR 점수 갱신
        normalUserService.updateMmr(dto.getUserId(), dto.getOpponentId(), gameHistory);

        return ResponseEntity.ok("경기 결과가 정상 반영되었습니다.");
    }
}
