package PL_25.shuttleplay.dto;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GameHistoryResponseDTO {
    private Long gameId;
    private String gameDate;
    private int scoreTeamA;
    private int scoreTeamB;
    private boolean isCompleted;
    private Long opponentId;
}

