package PL_25.shuttleplay.Controller;

import PL_25.shuttleplay.Entity.Game.Game;
import PL_25.shuttleplay.Entity.Game.GameHistory;
import PL_25.shuttleplay.Entity.Game.GameParticipant;
import PL_25.shuttleplay.Entity.Game.GameStatus;
import PL_25.shuttleplay.Entity.User.NormalUser;
import PL_25.shuttleplay.Repository.GameHistoryRepository;
import PL_25.shuttleplay.Repository.GameParticipantRepository;
import PL_25.shuttleplay.Repository.GameRepository;
import PL_25.shuttleplay.Repository.NormalUserRepository;
import PL_25.shuttleplay.Service.GameParticipantService;
import PL_25.shuttleplay.Service.MMRService;
import PL_25.shuttleplay.Service.NormalUserService;
import PL_25.shuttleplay.dto.GameHistoryDTO;
import PL_25.shuttleplay.dto.Matching.TeamAssignmentDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    private final NormalUserRepository normalUserRepository;
    private final GameRepository gameRepository;
    private final MMRService mmrService;
    private final GameParticipantService gameParticipantService;
    private GameHistory gameHistory;

    // 매칭된 게임(경기) 시작하기
    @PatchMapping("/{gameId}/start")
    public ResponseEntity<String> startGame(@PathVariable Long gameId) {
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new IllegalArgumentException("해당 gameId의 Game이 존재하지 않습니다"));

        if (game.getStatus() != GameStatus.WAITING) {
            return ResponseEntity.badRequest().body("이미 시작되었거나 유효하지 않은 상태입니다.");
        }

        game.setStatus(GameStatus.ONGOING);
        gameRepository.save(game);

        return ResponseEntity.ok("Game이 ONGOING 상태로 변경되었습니다.");
    }

    // 대기중인 매칭된 게임(경기) 취소하기
    @PatchMapping("/{gameId}/cancel")
    public ResponseEntity<String> cancelGame(@PathVariable Long gameId) {
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new IllegalArgumentException("해당 gameId의 Game이 존재하지 않습니다"));

        if (game.getStatus() != GameStatus.WAITING) {
            return ResponseEntity.badRequest().body("WAITING 상태의 게임만 취소할 수 있습니다.");
        }

        game.setStatus(GameStatus.CANCELLED);
        gameRepository.save(game);

        return ResponseEntity.ok("Game이 CANCELLED 상태로 변경되었습니다.");
    }

    // 게임 시작 전 팀 구분하기 (A팀, B팀)
    @PatchMapping("/{gameId}/team")
    public ResponseEntity<String> assignTeams(@PathVariable Long gameId, @RequestBody List<TeamAssignmentDTO> teamAssignments) {
        for (TeamAssignmentDTO dto : teamAssignments) {
            gameParticipantService.assignTeam(gameId, dto.getUserId(), dto.getParsedTeamType());
        }
        return ResponseEntity.ok("팀 배정 완료!");
    }

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
    // 경기 결과 입력 시 gameId는 입력하는 사용자가 참여중인 gameId를 자동으로 가져오도록 함
    @PostMapping("/result")
    public ResponseEntity<String> inputGameResult(@RequestBody GameHistoryDTO dto) {
        Long userId = dto.getUserId();  // 입력하는 사람
        // 입력하는 사용자가 참여한 지금 게임 찾기
        Game game = gameParticipantService.findFinishedGameByUser(userId)
                .orElseThrow(() -> new IllegalArgumentException(("사용자가 FINISHED 상태의 게임에 참여하고 있지 않습니다.")));

//        Game game = gameRepository.findById(dto.getGameId())
//                .orElseThrow(() -> new IllegalArgumentException("해당 gameId의 Game이 존재하지 않습니다 (2)"));

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

//        // Game 방장이 경기 결과를 입력하면 해당 Game에 참가중인 참가자들에게 모두 MMR 점수 반영하도록 함
//        if (game.getParticipants().size() != 2) {
//            return ResponseEntity.badRequest().body("현재는 1:1 경기만 지원됩니다.");
//        }

        // MMR 점수 갱신 (단식)
        Long userA = game.getParticipants().get(0).getUserId();
        Long userB = game.getParticipants().get(1).getUserId();
        // 복식 구분
        if (game.getParticipants().size() == 4) {
            mmrService.updateMmrForTeamMatch(game, gameHistory);
        }

        normalUserService.updateMmr(userA, userB, gameHistory);
        normalUserService.updateMmr(userB, userA, gameHistory);

        // 게임 히스토리 입력 후 participants 에게 할당되어있는 gameId null 처리 (참여중인 게임 해제)
        // 결과 반영 후 참가자들의 game 연결 해제
        List<GameParticipant> participants = gameParticipantService.findByGame(game);
        for (GameParticipant participant : participants) {
            Long userId1 = participant.getUser().getUserId();
            NormalUser user1 = normalUserRepository.findById(userId1)
                    .orElseThrow(() -> new IllegalArgumentException("not exist user"));
            user1.setCurrentGame(null);
            normalUserRepository.save(user1);
        }
        gameParticipantService.saveAll(participants);

        return ResponseEntity.ok("경기 결과가 정상 반영되었습니다.");
    }
}
