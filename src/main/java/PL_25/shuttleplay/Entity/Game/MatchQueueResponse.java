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
    private int matchedUserCount; // 게임방이 연결되어 있다면 참여자 수를 넣음

    public MatchQueueResponse(MatchQueueEntry entry) {
        this.userId = entry.getUser().getUserId();
        this.courtName = entry.getLocation() != null ? entry.getLocation().getCourtName() : null;
        this.courtAddress = entry.getLocation() != null ? entry.getLocation().getCourtAddress() : null;
        this.date = entry.getDate();
        this.time = entry.getTime();
        this.isPrematched = Boolean.TRUE.equals(entry.getIsPrematched());
        this.gameRoomId = entry.getGameRoom() != null ? entry.getGameRoom().getGameRoomId() : null;
        this.matchedUserCount = (entry.getGameRoom() != null && entry.getGameRoom().getParticipants() != null)
                ? entry.getGameRoom().getParticipants().size()
                : 1; // 자기 자신만 있을 경우 (대기 상태)
    }
}
