package PL_25.shuttleplay.Dto.Matching;

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

    private List<Long> userId;       // 선택된 사용자 ID 목록
    private boolean isLive;          // 현장매칭 여부
    private String courtName;
    private String courtAddress;
    private LocalDate date;          // isLive가 false일 때(사전 매칭)만 사용
    private LocalTime time;
}
