package PL_25.shuttleplay.Entity.Club;

import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class Notice {
    @Id
    public Long noticeId;

    private String title;
    private String content;
    private LocalDate date;
}
