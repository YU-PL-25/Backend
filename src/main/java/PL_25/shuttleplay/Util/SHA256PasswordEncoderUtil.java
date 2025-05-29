package PL_25.shuttleplay.Util;

import com.google.common.hash.Hashing;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class SHA256PasswordEncoderUtil {

    private static final SecureRandom random = new SecureRandom();


    public static String sha256WithSaltEncode(String password, String salt) {

        return Hashing.sha256().hashString(password + salt, StandardCharsets.UTF_8).toString();
    }


    public static String getSalt() {

        byte[] salt = new byte[16];
        random.nextBytes(salt);

        return Base64.getEncoder().encodeToString(salt);
    }
}
