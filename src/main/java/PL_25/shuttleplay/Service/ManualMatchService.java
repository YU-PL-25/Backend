package PL_25.shuttleplay.Service;

import PL_25.shuttleplay.Entity.Game.Game;
import PL_25.shuttleplay.Entity.Game.GameRoom;
import PL_25.shuttleplay.Entity.Game.MatchQueueEntry;
import PL_25.shuttleplay.Entity.Location;
import PL_25.shuttleplay.Repository.GameRepository;
import PL_25.shuttleplay.Repository.GameRoomRepository;
import PL_25.shuttleplay.Repository.MatchQueueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ManualMatchService {

    private final GameRoomRepository gameRoomRepository;
    private final GameRepository gameRepository;
    private final MatchQueueRepository matchQueueRepository;

    /**
     * 사전 수동 매칭 (구장 기준)
     * → 큐에 등록된 userId 기반으로 게임 생성
     */
    public Game createManualGameFromRoom(GameRoom room, List<Long> userIds, LocalDate date, LocalTime time) {
        List<MatchQueueEntry> entries = matchQueueRepository.findByUser_UserIdInAndMatchedFalse(userIds);

        if (entries.size() != userIds.size()) {
            throw new IllegalArgumentException("일부 사용자가 매칭 큐에 없거나 이미 매칭되었습니다.");
        }

        entries.forEach(e -> {
            e.setMatched(true);
            e.setIsPrematched(true);
        });
        matchQueueRepository.saveAll(entries);

        Game game = new Game();
        game.setDate(date != null ? date : LocalDate.now());
        game.setTime(time != null ? time : LocalTime.now());
        game.setLocation(room.getLocation());
        game.setParticipants(entries.stream().map(MatchQueueEntry::getUser).toList());

        return gameRepository.save(game);
    }

    /**
     * 현장 수동 매칭 (구장 기준)
     * → 위 메서드와 동일하지만 isPrematched = false
     */
    public Game createLiveGameFromRoom(GameRoom room, List<Long> userIds) {
        List<MatchQueueEntry> entries = matchQueueRepository.findByUser_UserIdInAndMatchedFalse(userIds);

        if (entries.size() != userIds.size()) {
            throw new IllegalArgumentException("일부 사용자가 매칭 큐에 없거나 이미 매칭되었습니다.");
        }

        entries.forEach(e -> {
            e.setMatched(true);
            e.setIsPrematched(false);
        });
        matchQueueRepository.saveAll(entries);

        Game game = new Game();
        game.setDate(LocalDate.now());
        game.setTime(LocalTime.now());
        game.setLocation(room.getLocation());
        game.setParticipants(entries.stream().map(MatchQueueEntry::getUser).toList());

        return gameRepository.save(game);
    }

    /**
     * 사전 수동 매칭 (동네 기준)
     * → 큐에 등록된 모든 미매칭 유저 대상으로 방 생성
     */
    public GameRoom createManualMeetingRoomFromQueue(String courtName, String courtAddress, LocalDate date, LocalTime time) {
        List<MatchQueueEntry> entries = matchQueueRepository.findByMatchedFalse();

        if (entries.isEmpty()) {
            throw new IllegalArgumentException("매칭 큐에 유저가 없습니다.");
        }

        entries.forEach(e -> {
            e.setMatched(true);
            e.setIsPrematched(true);
        });
        matchQueueRepository.saveAll(entries);

        Location location = new Location();
        location.setCourtName(courtName);
        location.setCourtAddress(courtAddress);

        GameRoom room = new GameRoom();
        room.setLocation(location);
        room.setDate(date);
        room.setTime(time);
        room.setParticipants(entries.stream().map(MatchQueueEntry::getUser).toList());

        return gameRoomRepository.save(room);
    }
}
