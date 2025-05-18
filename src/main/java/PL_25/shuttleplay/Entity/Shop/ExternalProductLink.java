package PL_25.shuttleplay.Entity.Shop;

import PL_25.shuttleplay.Entity.Shop.Product;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ExternalProductLink {
    @Id
    public Product productId;

    private String productName;
    private String externalUrl;
}
