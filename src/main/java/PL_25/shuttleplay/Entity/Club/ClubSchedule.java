package PL_25.shuttleplay.Entity.Club;

import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class ClubSchedule {
    @Id
    public Long scheduleId;

    private LocalDate scheduleDate;
    private String description;
}
