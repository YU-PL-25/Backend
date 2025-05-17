package PL_25.shuttleplay.Entity.Club;

import PL_25.shuttleplay.Entity.User.NormalUser;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class Club {
    @Id
    private Long clubId;

    private String name;
    private List<NormalUser> members;
    private NormalUser admin;
    private Notice[] notices;
    private ClubSchedule schedule;
    private String inviteLink;
    private JoinRequest request;
}
