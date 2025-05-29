package PL_25.shuttleplay.Controller;

import PL_25.shuttleplay.Entity.User.NormalUser;
import PL_25.shuttleplay.Service.LoginService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;



@RestController
@RequiredArgsConstructor
public class LoginController {

    private final LoginService loginService;


    // 로그인 요청
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> processLogin(
            @RequestBody Map<String, Object> loginDto,
            HttpSession httpSession
    ) {
        String id = (String) loginDto.get("id");
        String password = (String) loginDto.get("password");

        Map<String, Object> response = new HashMap<>();

        System.out.println("받기 성공");
        try {

            NormalUser loginUser = loginService.checkIsValidUser(id, password);
            if (loginUser != null) {
                httpSession.setAttribute("loginUser", loginUser.getUserId());
                System.out.println("로그인 성공!!");
            }
            // 세션 추가 코드...
            return null;
        } catch (Exception e) {
            return null;
        }
    }


    // 로그아웃 요청
    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout() {



        return null;
    }
}
