package PL_25.shuttleplay.Entity.Club;

import PL_25.shuttleplay.Entity.User.User;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class JoinRequest {
    @Id
    public Long requestId;

    private User requester; // 초대를 보낸 사용자
    private boolean isApproved;

}
