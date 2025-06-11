package PL_25.shuttleplay.Repository;

import PL_25.shuttleplay.Entity.Game.MatchQueueEntry;
import PL_25.shuttleplay.Entity.Game.MatchQueueType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface MatchQueueRepository extends JpaRepository<MatchQueueEntry, Long> {

    // 1. 해당 게임방에 매칭되지 않은 사용자 목록 (구장 기준)
    List<MatchQueueEntry> findByMatchedFalseAndGameRoom_GameRoomId(Long roomId);

    // 2. 전체 매칭 큐에서 특정 매칭 타입이면서 매칭되지 않은 사용자 목록
    List<MatchQueueEntry> findByMatchedFalseAndMatchType(MatchQueueType matchType);

    // 3. 특정 매칭 타입이면서 매칭되지 않았고 게임방이 없는 사용자 목록
    List<MatchQueueEntry> findByMatchedFalseAndMatchTypeAndGameRoomIsNull(MatchQueueType matchType);

    // 4. 여러 명의 userId를 기준으로 매칭되지 않은 큐 엔트리들 조회
    List<MatchQueueEntry> findByUser_UserIdInAndMatchedFalse(List<Long> userIds);

    // 5. 단일 userId에 대해 매칭되지 않은 큐 엔트리들 조회
    List<MatchQueueEntry> findByUser_UserIdAndMatchedFalse(Long userId);

    // 6. 해당 유저가 다른 게임방에 매칭 큐로 등록되어 있는지 여부
    boolean existsByUser_UserIdAndMatchedFalseAndGameRoomIsNotNull(Long userId);

    // 7. 특정 게임방에서 해당 유저가 매칭 큐에 등록된 상태인지 확인
    boolean existsByUser_UserIdAndGameRoom_GameRoomIdAndMatchedFalse(Long userId, Long roomId);

    // 8. 해당 유저가 게임방 없이 동네(위치 기반) 매칭 큐에 등록되어 있는지 여부
    boolean existsByUser_UserIdAndMatchedFalseAndGameRoomIsNull(Long userId);

    // 9 해당 게임방을 가지는 유저 모두 조회
    List<MatchQueueEntry> findByGameRoom_GameRoomIdAndMatchedFalse(Long gameRoomId);

    List<MatchQueueEntry> findByUser_UserIdAndMatchedFalseAndMatchType(Long userId, MatchQueueType matchQueueType);
}


