package PL_25.shuttleplay.Controller;

import PL_25.shuttleplay.Entity.Game.MatchQueueEntry;
import PL_25.shuttleplay.Entity.Game.MatchQueueType;
import PL_25.shuttleplay.Entity.User.NormalUser;
import PL_25.shuttleplay.Repository.MatchQueueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/match/common")
@RequiredArgsConstructor
public class CommonMatchController {

    private final MatchQueueRepository matchQueueRepository;

    // 특정 게임방에 등록된 모든 매칭 큐 사용자 조회 (자동/수동 통합)
    @GetMapping("/queue-users/live")
    public ResponseEntity<Map<String, Object>> getLiveQueueUsersByRoom(@RequestParam Long roomId) {
        // 해당 게임방의 미매칭 상태인 매칭 큐 사용자 중에서 현장 매칭(QUEUE_LIVE)인 경우만 필터링
        List<MatchQueueEntry> entries = matchQueueRepository.findByMatchedFalseAndGameRoom_GameRoomId(roomId)
                .stream()
                .filter(entry -> entry.getMatchType() == MatchQueueType.QUEUE_LIVE)  // 변경된 enum 값으로 비교
                .toList();

        // 사용자 정보를 JSON 형태로 변환
        List<Map<String, Object>> userList = entries.stream().map(entry -> {
            NormalUser user = entry.getUser();
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("userId", user.getUserId());
            userInfo.put("nickname", user.getNickname());
            userInfo.put("rank", user.getRank());
            userInfo.put("matchType", entry.getMatchType().name());
            userInfo.put("isPrematched", entry.getIsPrematched());
            return userInfo;
        }).toList();

        // 응답 JSON 반환
        return ResponseEntity.ok(Map.of(
                "roomId", roomId,
                "queuedUsers", userList
        ));
    }

}
