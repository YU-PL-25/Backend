package PL_25.shuttleplay.Repository;

import PL_25.shuttleplay.Entity.Location;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LocationRepository extends JpaRepository<Location, Long> {

    Location findByLatitudeAndLongitude(double latitude, double longitude);
}
