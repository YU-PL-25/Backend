package PL_25.shuttleplay.dto.Matching;

import PL_25.shuttleplay.Entity.Location;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

// 수동 매칭 요청 시 필요 입력값 객체

@Getter
@Setter
@RequiredArgsConstructor
public class ManualMatchRequest {

    // 선택된 사용자 ID 목록
    private List<Long> userId;

    // 위치 정보
    private Location location;

    private LocalDate date;          //  사전 매칭만 사용
    private LocalTime time;          //  사전 매칭만 사용
    public boolean isPreMatch(){ // 사전매칭 여부
        return date != null && time != null;
    }
}
