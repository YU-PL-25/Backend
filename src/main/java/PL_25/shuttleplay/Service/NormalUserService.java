package PL_25.shuttleplay.Service;

import PL_25.shuttleplay.Entity.Game.GameHistory;
import PL_25.shuttleplay.Entity.User.MMR;
import PL_25.shuttleplay.Entity.User.NormalUser;
import PL_25.shuttleplay.Entity.User.Profile;
import PL_25.shuttleplay.Entity.User.Rank;
import PL_25.shuttleplay.Repository.NormalUserRepository;
import PL_25.shuttleplay.dto.UserRegisterDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import static PL_25.shuttleplay.Util.SHA256PasswordEncoderUtil.sha256WithSaltEncode;
import static PL_25.shuttleplay.Util.SHA256PasswordEncoderUtil.getSalt;


@Service
@RequiredArgsConstructor
public class NormalUserService {

    private final NormalUserRepository normalUserRepository;
    private final MMRService mmrService;

    // 사용자 회원가입(등록)
    // 급수에 따른 MMR 초기화도 이때 진행
    public NormalUser createUser(UserRegisterDTO dto) {

        String salt = getSalt();
        String encodedPassword = sha256WithSaltEncode(dto.getPassword(), salt);
        dto.setRole("normal");

        NormalUser user = new NormalUser();
        user.setName(dto.getName());
        user.setNickname(dto.getNickname());
        user.setGender(dto.getGender());

        user.setLoginId(dto.getLoginId()); // 아이디 설정
        user.setPassword(encodedPassword); // 암호화된 패스워드 설정
        user.setSalt(salt); // 로그인 하기위해 salt 설정.

        user.setEmail(dto.getEmail());
        user.setPhone(dto.getPhone());
        user.setRole(dto.getRole()); // 일반 사용자 설정

        Rank rank = Rank.fromString(dto.getRankStr());
        user.setRank(rank);

        MMR mmr = mmrService.createInitialMmr(rank); // 등급에 따라 초기화
        user.setMmr(mmr);

        Profile profile = new Profile(
                dto.getAgeGroup(),
                dto.getPlayStyle(),
                dto.getGameType()
        );
        user.setProfile(profile);

        return normalUserRepository.save(user);
    }

    // 사용자 조회
    public NormalUser findUserById(Long userId) {
        return normalUserRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("해당 유저를 찾을 수 없습니다."));
    }

    // 경기 결과 반영 (MMR 갱신)
    public void updateMmr(Long userId, Long opponentId, GameHistory gameHistory) {
        NormalUser user = findUserById(userId);
        NormalUser opponent = findUserById(opponentId);

        mmrService.updateMmr(user, opponent, gameHistory);   // 계산하기
        normalUserRepository.save(user); // MMR 포함 저장
    }
}
