package PL_25.shuttleplay.Controller;

import PL_25.shuttleplay.Util.TmapAPIAdapter;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class LocationController {
    private final TmapAPIAdapter tmapAPIAdapter;

    @GetMapping("/api/location/nearby")
    public List<String> getNearby(@RequestParam String keyword) {
        return tmapAPIAdapter.getNearbyCourts(keyword);
    }
}
