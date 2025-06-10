package PL_25.shuttleplay.dto.Matching;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class GameRoomDTO {
    private Long gameRoomId;
    private Long managerId;  // 방장 아이디
    private String title;    // 게임방 제목
    private String locationName;    // 장소 이름 (ex: ~~센터)
    private String locationAddress; // 장소 주소 (ex: 경산시 압량읍 ~~)
    private List<GameDTO> games;  // 해당 게임방의 게임 리스트
    private int participantCount; // 게임방 참여 인원수
}
