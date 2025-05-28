package PL_25.shuttleplay.Repository;

import PL_25.shuttleplay.Entity.Game.MatchQueueEntry;
import PL_25.shuttleplay.Entity.Game.MatchQueueType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MatchQueueRepository extends JpaRepository<MatchQueueEntry, Long> {

    // 구장 기준: 해당 방의 매칭 안 된 전체 큐
    List<MatchQueueEntry> findByMatchedFalseAndGameRoom_GameRoomId(Long roomId);

    // 전체 큐에서 아직 매칭되지 않은 모든 사용자
    List<MatchQueueEntry> findByMatchedFalseAndMatchType(MatchQueueType matchType);

    // 특정 매칭 타입에 해당하면서, 매칭도 안 되었고, 게임방에도 배정되지 않은 사용자들을 조회
    List<MatchQueueEntry> findByMatchedFalseAndMatchTypeAndGameRoomIsNull(MatchQueueType matchType);

    // 유저 ID로 큐 검색 (게임방 관계 무시)
    List<MatchQueueEntry> findByUser_UserIdInAndMatchedFalse(List<Long> userIds);

    // 단일 userId용 추가
    List<MatchQueueEntry> findByUser_UserIdAndMatchedFalse(Long userId);

    // 매칭 안되고, 다른 게임방 큐에 대기중이지 않고, 진행중인 게임에 참여하는지
    boolean existsByUser_UserIdAndMatchedFalseAndGameRoomIsNotNull(Long userId);

    // 특정 게임방에 해당 유저가 매칭 큐에 등록되어 있는지
    boolean existsByUser_UserIdAndGameRoom_GameRoomIdAndMatchedFalse(Long userId, Long roomId);

    // 기존 동네 큐 등록 여부
    boolean existsByUser_UserIdAndMatchedFalseAndGameRoomIsNull(Long userId);

}


