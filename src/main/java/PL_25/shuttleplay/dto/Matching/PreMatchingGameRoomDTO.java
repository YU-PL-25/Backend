package PL_25.shuttleplay.dto.Matching;

import PL_25.shuttleplay.Entity.Location;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@NoArgsConstructor
public class PreMatchingGameRoomDTO {

    private Long masterId;
    private Location location;
    private LocalDate localDate;
    private LocalTime localTime;
}
