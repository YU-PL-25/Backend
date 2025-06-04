package PL_25.shuttleplay.Service;

import PL_25.shuttleplay.Entity.User.NormalUser;
import PL_25.shuttleplay.Repository.NormalUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;
import java.util.Optional;

import static PL_25.shuttleplay.Util.SHA256PasswordEncoderUtil.sha256WithSaltEncode;
import static PL_25.shuttleplay.Util.SHA256PasswordEncoderUtil.getSalt;

@Service
@RequiredArgsConstructor
public class LoginService {

    private final NormalUserRepository normalUserRepository;


    @Transactional(readOnly = true)
    public NormalUser checkIsValidUser(String email, String password) {

        // id로 일단 유저 정보 가져오기
        NormalUser user = Optional.ofNullable(normalUserRepository.findFirstByEmail(email))
                .orElseThrow(() -> new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다."));

        // salt 값 가져와서
        String salt = user.getSalt();
        // 사용자가 입력한 패스워드 암호화 하기.
        String encodedPassword = sha256WithSaltEncode(password, salt);
        // db에 저장된 값과 사용자가 입력한 패스워드를 암호화한 것과 같으면 ok.
        if (!user.getPassword().equals(encodedPassword)) {
            throw new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        return user;
    }
}
