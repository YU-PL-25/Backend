package PL_25.shuttleplay.Repository;

import PL_25.shuttleplay.Entity.Game.MatchQueueEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MatchQueueRepository extends JpaRepository<MatchQueueEntry, Long> {

    // 구장 기준 매칭용
    List<MatchQueueEntry> findByMatchedFalseAndGameRoom_GameRoomId(Long roomId);

    // 전체 큐 조회용 (동네 기준 등)
    List<MatchQueueEntry> findByMatchedFalse();

    // 수동 매칭용: userId 리스트 기반으로 아직 매칭 안 된 큐 엔트리 찾기
    List<MatchQueueEntry> findByUser_UserIdInAndMatchedFalse(List<Long> userIds);
}

