package PL_25.shuttleplay.Entity.Reservation;

import PL_25.shuttleplay.Entity.Location;
import jakarta.persistence.Id;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

// 여기서 Court는 해당 구장을 나타내는지...

@Getter
@Setter
public class Court {
    @Id
    public Long courtId;

    private String courtName;
    private Location courtLoc;
    private List<Court> availableSlots; // 해당 구장의 이용가능한 코트?
}
