package PL_25.shuttleplay.Entity.User;

import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class Admin extends User {
//    @Id
//    public long userId;

    private String role;
    private List<String> report;    // 신고접수 받은 항목들
}
