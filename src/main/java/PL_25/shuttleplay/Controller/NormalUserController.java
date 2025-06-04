package PL_25.shuttleplay.Controller;

import PL_25.shuttleplay.dto.GameHistoryResponseDTO;
import PL_25.shuttleplay.Entity.Game.GameHistory;
import PL_25.shuttleplay.Entity.User.NormalUser;
import PL_25.shuttleplay.Repository.GameHistoryRepository;
import PL_25.shuttleplay.Service.NormalUserService;
import PL_25.shuttleplay.dto.UserRegisterDTO;
import jakarta.servlet.http.HttpSession;
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

    // 사용자 개인정보 조회
    @GetMapping("/myinfo")
    public NormalUser getMyInfo(HttpSession session) {
        Long userId = (Long) session.getAttribute("loginUser");
        if (userId == null) {
            throw new RuntimeException("로그인 세션이 존재하지 않습니다.");
        }
        return normalUserService.findUserById(userId);
    }

    @GetMapping("/{userId}/game-history")
    public List<GameHistoryResponseDTO> getGameHistory(@PathVariable Long userId) {
        List<GameHistory> histories = gameHistoryRepository.findByUserId(userId);

        return histories.stream()
            .map(history -> {
                // 상대방 ID 추출 로직 수정!
                Long opponentId = history.getGame().getParticipants().stream()
                        .filter(p -> !p.getUser().getUserId().equals(userId))
                        .findFirst()
                        .map(participant -> participant.getUser().getUserId())
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
