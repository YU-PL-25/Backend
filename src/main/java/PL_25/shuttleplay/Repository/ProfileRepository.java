package PL_25.shuttleplay.Repository;

import PL_25.shuttleplay.Entity.User.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProfileRepository extends JpaRepository<Profile, Long> {
    Optional<Profile> findFirstByGameTypeAndAgeGroupAndPlayStyle(String gameType, String ageGroup, String playStyle);
}

