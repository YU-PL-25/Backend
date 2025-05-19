package PL_25.shuttleplay.Controller;

import PL_25.shuttleplay.Entity.Game.Game;
import PL_25.shuttleplay.Entity.Game.GameHistory;
import PL_25.shuttleplay.Entity.Game.GameStatus;
import PL_25.shuttleplay.Entity.User.NormalUser;
import PL_25.shuttleplay.Repository.GameHistoryRepository;
import PL_25.shuttleplay.Repository.GameRepository;
import PL_25.shuttleplay.Service.MMRService;
import PL_25.shuttleplay.Service.NormalUserService;
import PL_25.shuttleplay.dto.GameHistoryDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    private final GameRepository gameRepository;
    private final MMRService mmrService;

    // 경기 종료 시 FINISHED 상태로 전환 (게임 종료하기, 스코어 입력하기)
    @PatchMapping("/{gameId}/complete")
    public ResponseEntity<String> completeGame(@PathVariable Long gameId) {
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new IllegalArgumentException("해당 gameId의 Game이 존재하지 않습니다"));

        game.setStatus(GameStatus.FINISHED);
        gameRepository.save(game);

        return ResponseEntity.ok("Game이 FINISHED 상태로 변경되었습니다.");
    }

    // 경기 결과 입력 및 MMR 점수 갱신
    @PostMapping("/result")
    public ResponseEntity<String> inputGameResult(@RequestBody GameHistoryDTO dto) {
        Game game = gameRepository.findById(dto.getGameId())
                .orElseThrow(() -> new IllegalArgumentException("해당 gameId의 Game이 존재하지 않습니다 (2)"));

        // 종료되지 않은 게임은 결과 입력 불가능
        if (game.getStatus() != GameStatus.FINISHED) {
            throw new IllegalStateException("아직 종료되지 않은 게임입니다. 결과를 입력할 수 없습니다.");
        }

        // 중복 입력 방지
        if (game.getGameHistory() != null) {
            throw new IllegalArgumentException("이미 해당 게임의 결과가 입력되었습니다.");
        }

        // dto로부터 입력받은 정보를 가지고 GameHistory 객체 생성
        // Game 과 GameHistory 양방향 연걸
        GameHistory gameHistory = dto.toGameHistory();
        game.setGameHistory(gameHistory);
        gameHistory.setGame(game);

        // GameHistory 먼저 저장
        gameHistoryRepository.save(gameHistory);
        gameRepository.save(game);

        // MMR 점수 갱신
//        normalUserService.updateMmr(dto.getUserId(), dto.getOpponentId(), gameHistory);

        // Game 방장이 경기 결과를 입력하면 해당 Game에 참가중인 참가자들에게 모두 MMR 점수 반영하도록 함
        if (game.getParticipants().size() != 2) {
            return ResponseEntity.badRequest().body("현재는 1:1 경기만 지원됩니다.");
        }
        // MMR 점수 갱신 (단식)
        Long userA = game.getParticipants().get(0).getUserId();
        Long userB = game.getParticipants().get(1).getUserId();

        normalUserService.updateMmr(userA, userB, gameHistory);
        normalUserService.updateMmr(userB, userA, gameHistory);

        return ResponseEntity.ok("경기 결과가 정상 반영되었습니다.");
    }
}
