package PL_25.shuttleplay.Entity.Reservation;

import PL_25.shuttleplay.Entity.User.User;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

// 예약 시간대 표현을 위한 열거형 클래스
enum TimeSlot {
    // 1시간 단위로, 9시부터 4시까지 표현
    SLOT_09_10,
    SLOT_10_11,
    SLOT_11_12,
    SLOT_13_14,
    SLOT_14_15,
    SLOT_15_16
}

@Getter
@Setter
public class Reservation {
    @Id
    public Long reservationId;

    private User reservationUser;   // 예약을 한 사용자(=신청한사람)
    private Court reservationCourt; // 어느 구장(코트)를 예약한건지
    private LocalDate reservationDate;  // 예약 날짜
    private TimeSlot reservationTime;   // 몇시간동안 사용할건지 시간
    private boolean isApproved;         // 예약 완료,거부
    private Payment payment;        // 결제 관련
}
