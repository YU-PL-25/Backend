package PL_25.shuttleplay.Entity.User;

import jakarta.persistence.Id;

public class NormalUser extends User {
    @Id
    public long userId;

    private String name;
    private String email;
    private String phone;
    private String rank;
    private MMR mmr;    // mmr 점수
    private Profile profile;    // 개인 게임 정보를 담은 프로필 객체
    private String role;    // admin(?? 관리자), normal(일반 사용자), manager(방 관리자)
}
