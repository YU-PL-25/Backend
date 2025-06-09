package PL_25.shuttleplay.Controller;

import PL_25.shuttleplay.Entity.User.NormalUser;
import PL_25.shuttleplay.Service.LoginService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
        String email = (String) loginDto.get("email");
        String password = (String) loginDto.get("password");

        Map<String, Object> response = new HashMap<>();

        try {

            // 해당 유저가 유효한 유저인지 확인.
            NormalUser loginUser = loginService.checkIsValidUser(email, password);

            // 해당 유저 pk로 session 설정, 세션 관련 설정은 CustomSessionListener 참고.
            httpSession.setAttribute("loginUser", loginUser.getUserId());

            response.put("status", 200);
            response.put("message", "로그인에 성공했습니다.");
            response.put("userId", loginUser.getUserId());
            return ResponseEntity.status(HttpStatus.OK).body(response);

        } catch (IllegalArgumentException e) {
            response.put("status", 401);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        } catch (Exception e) {
            response.put("status", 500);
            response.put("error", "서버 오류가 발생했습니다.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }


    // 로그아웃 요청
    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> processLogout(HttpSession httpSession) {

        Map<String, Object> response = new HashMap<>();

        Long loginUser = (Long) httpSession.getAttribute("loginUser");

        if (loginUser != null) {
            httpSession.invalidate(); // 세션 무효화.
            response.put("status", 200);
            response.put("message", "로그아웃 성공");
            return ResponseEntity.status(HttpStatus.OK).body(response);

        }
        else {
            response.put("status", 400);
            response.put("error", "로그인 세션이 존재하지 않습니다.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }
}
