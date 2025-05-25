package PL_25.shuttleplay.Service;

import PL_25.shuttleplay.Entity.Location;
import PL_25.shuttleplay.Repository.LocationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LocationService {

    private final LocationRepository locationRepository;


    // 해당 위도(Latitude), 경도(Longitude)가 이미 있는지 확인
    @Transactional
    public Location findOrCreateLocation(Location location) {

        try {
            Location existedLocation = locationRepository
                    .findByLatitudeAndLongitude(location.getLatitude(), location.getLongitude());

            // 이미 존재하면 return
            if (existedLocation != null) {
                return existedLocation;
            }
            // 없으면 삽입하고 return
            return locationRepository.save(location);
        } catch (Exception e) {
            return null;
        }
    }

}
