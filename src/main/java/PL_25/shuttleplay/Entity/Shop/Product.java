package PL_25.shuttleplay.Entity.Shop;

import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Product {
    @Id
    public Long productId;

    @ManyToOne
    @JoinColumn(name = "shopId", nullable = false)
    private Shop shopId;
    private String shopName;
    private int price;
    private ExternalProductLink link;
}
