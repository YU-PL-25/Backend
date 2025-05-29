package PL_25.shuttleplay.dto;

import lombok.Getter;
import lombok.Setter;

/*********************************************
* 회원가입 시 클라이언트로부터 정보를 입력받아 전달하는 dto
* ********************************************/

@Getter
@Setter
public class UserRegisterDTO {
    // NormalUser 객체에 담길 정보
    private String name;
    private String loginId;
    private String password;
    private String nickname;
    private String gender;
    private String email;
    private String phone;
    private String rankStr;    // S, A, B, C
    private String role;    // normal, admin

    // Profile 객체에 담길 정보
    private String ageGroup;
    private String playStyle;
    private String gameType;
}
