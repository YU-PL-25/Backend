package PL_25.shuttleplay.Entity.Game;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
public class MatchQueueResponse {

    private Long userId;
    private String courtName;
    private String courtAddress;
    private LocalDate date;
    private LocalTime time;
    private boolean isPrematched;
    private Long gameRoomId;

    public MatchQueueResponse(MatchQueueEntry entry) {
        this.userId = entry.getUser().getUserId();
        this.courtName = entry.getLocation() != null ? entry.getLocation().getCourtName() : null;
        this.courtAddress = entry.getLocation() != null ? entry.getLocation().getCourtAddress() : null;
        this.date = entry.getDate();
        this.time = entry.getTime();
        this.isPrematched = Boolean.TRUE.equals(entry.getIsPrematched());
        this.gameRoomId = entry.getGameRoom() != null ? entry.getGameRoom().getGameRoomId() : null;
    }
}
