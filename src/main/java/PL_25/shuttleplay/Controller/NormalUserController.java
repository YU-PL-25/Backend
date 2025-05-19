package PL_25.shuttleplay.Controller;

import PL_25.shuttleplay.dto.GameHistoryResponseDTO;
import PL_25.shuttleplay.Entity.Game.GameHistory;
import PL_25.shuttleplay.Entity.User.NormalUser;
import PL_25.shuttleplay.Repository.GameHistoryRepository;
import PL_25.shuttleplay.Service.NormalUserService;
import PL_25.shuttleplay.dto.UserRegisterDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class NormalUserController {
    private final NormalUserService normalUserService;
    private final GameHistoryRepository gameHistoryRepository;

    // 사용자 회원가입 시 정보 입력 및 MMR 점수 초기화
    @PostMapping("/register")
    public NormalUser registerUser(@RequestBody UserRegisterDTO registerDTO) {
        return normalUserService.createUser(registerDTO);
    }

    @GetMapping("/{userId}/game-history")
    public List<GameHistoryResponseDTO> getGameHistory(@PathVariable Long userId) {
        List<GameHistory> histories = gameHistoryRepository.findByUserId(userId);

        return histories.stream()
            .map(history -> {
                // 상대방 ID 추출 로직 예시
                Long opponentId = history.getGame().getParticipants().stream()
                    .filter(p -> !p.getUserId().equals(userId))
                    .findFirst()
                    .map(NormalUser::getUserId)
                    .orElse(null);

                return GameHistoryResponseDTO.builder()
                    .gameId(history.getGame().getGameId())
                    .gameDate(history.getGame().getDate().toString())
                    .scoreTeamA(history.getScoreTeamA())
                    .scoreTeamB(history.getScoreTeamB())
                    .isCompleted(history.isCompleted())
                    .opponentId(opponentId)
                    .build();
            })
            .toList();
    }

}
