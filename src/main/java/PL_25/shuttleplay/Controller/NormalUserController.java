package PL_25.shuttleplay.Controller;

import PL_25.shuttleplay.Entity.User.NormalUser;
import PL_25.shuttleplay.Repository.NormalUserRepository;
import PL_25.shuttleplay.Service.NormalUserService;
import PL_25.shuttleplay.dto.UserRegisterDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class NormalUserController {
    private final NormalUserService normalUserService;

    // 사용자 회원가입 시 정보 입력 및 MMR 점수 초기화
    @PostMapping("/register")
    public NormalUser registerUser(@RequestBody UserRegisterDTO registerDTO) {
        return normalUserService.createUser(registerDTO);
    }
}
