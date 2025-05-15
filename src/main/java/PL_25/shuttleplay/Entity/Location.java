package PL_25.shuttleplay.Entity;

import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Location {
    @Id
    public long locationId;

    private String userLocation;
    private TmapAPIAdapter adapter;

}

