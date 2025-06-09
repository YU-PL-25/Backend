package PL_25.shuttleplay.Entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.*;

@Getter
@Setter
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Location {
    @Id
    @GeneratedValue()
    public Long locationId;

    private String userLocation; // 사용자 현재 위치 (GPS 기반)

    private double latitude;       // 위도
    private double longitude;      // 경도

    private String courtName;      // (선택된 경우) 구장명
    private String courtAddress;   // (선택된 경우) 구장주소

    public Location(String courtName, String courtAddress, double latitude, double longitude) {
        this.courtName = courtName;
        this.courtAddress = courtAddress;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public Location(String courtName, String courtAddress) {
        this.courtName = courtName;
        this.courtAddress = courtAddress;
    }

}

