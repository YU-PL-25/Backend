package PL_25.shuttleplay.Entity.Reservation;

import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Payment {
    @Id
    public Long paymentId;

    private String method;  // 결제 수단
    private boolean isPaid; // 결제 완료 여부
}
