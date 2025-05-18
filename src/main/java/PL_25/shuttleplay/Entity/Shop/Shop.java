package PL_25.shuttleplay.Entity.Shop;

import PL_25.shuttleplay.Entity.Location;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class Shop {
    @Id
    public Long shopId;

    private String shopName;
    private String shopURL;
    private List<Product> productList;
    private Location location;;
}
