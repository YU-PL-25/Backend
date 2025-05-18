package PL_25.shuttleplay.Repository;

import PL_25.shuttleplay.Entity.User.MMR;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MMRRepository extends JpaRepository<MMR, Long> {
    Optional<MMR> findByRatingAndTolerance(int rating, int tolerance);
}


