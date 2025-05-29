package PL_25.shuttleplay.dto.Matching;

import PL_25.shuttleplay.Entity.Location;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

// 자동 매칭 요청 시 필요 입력값 객체

@Getter
@Setter
@RequiredArgsConstructor
public class AutoMatchRequest {
    // 사용자 아이디
    // 동네 매칭 GameRoom 참여자 저장
    private List<Long> userId;

    // 사전매칭용
    private LocalDate date;
    private LocalTime time;

    // 위치 정보
    private Location location;

    // 프로필 정보
    private ProfileDTO profile;

    // mmr 정보
    private MMRDTO mmr;

   // 원하는 게임방 매칭 총 인원 수 (본인 포함)
    private int requiredMatchCount;

    public boolean isPreMatch(){
        return date != null && time != null;
    }
}
