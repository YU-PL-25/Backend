package PL_25.shuttleplay.Service;

import PL_25.shuttleplay.Dto.Matching.ManualMatchRequest;
import PL_25.shuttleplay.Entity.Game.*;
import PL_25.shuttleplay.Entity.Location;
import PL_25.shuttleplay.Entity.User.NormalUser;
import PL_25.shuttleplay.Repository.GameRepository;
import PL_25.shuttleplay.Repository.GameRoomRepository;
import PL_25.shuttleplay.Repository.MatchQueueRepository;
import PL_25.shuttleplay.Repository.NormalUserRepository;
import PL_25.shuttleplay.Util.GeoUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ManualMatchService {

    private final GameRoomRepository gameRoomRepository;
    private final GameRepository gameRepository;
    private final MatchQueueRepository matchQueueRepository;
    private final NormalUserRepository normalUserRepository;

    private void validateUsersBeforeMatch(List<Long> userIds, GameRoom currentRoomOrNull) {
        for (Long userId : userIds) {
            boolean inGame = gameRepository.existsByParticipants_UserIdAndStatus(userId, GameStatus.ONGOING);
            if (inGame) {
                throw new IllegalStateException("userId=" + userId + " 는 이미 게임 중입니다.");
            }
            List<MatchQueueEntry> existing = matchQueueRepository.findByUser_UserIdAndMatchedFalse(userId);
            for (MatchQueueEntry entry : existing) {
                if (entry.getGameRoom() != null &&
                        (currentRoomOrNull == null || !entry.getGameRoom().getGameRoomId().equals(currentRoomOrNull.getGameRoomId()))) {
                    throw new IllegalStateException("userId=" + userId + " 는 다른 방에 대기 중입니다.");
                }
            }
        }
    }
    // 매칭 큐 등록
    public MatchQueueResponse registerToQueue(Long userId, ManualMatchRequest request) {
        NormalUser user = normalUserRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없음"));

        if (request.getLocation().getCourtName() == null || request.getLocation().getCourtAddress() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "코트 이름과 주소는 필수입니다.");
        }

        Long roomId = user.getGameRoom() != null ? user.getGameRoom().getGameRoomId() : null;

        if (roomId != null && matchQueueRepository.existsByUser_UserIdAndGameRoom_GameRoomIdAndMatchedFalse(userId, roomId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 해당 게임방에 매칭 등록이 되어있습니다.");
        }

        if (matchQueueRepository.existsByUser_UserIdAndMatchedFalseAndGameRoomIsNotNull(userId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 다른 게임방에 매칭 등록이 되어있습니다.");
        }

        MatchQueueEntry entry = new MatchQueueEntry();
        entry.setUser(user);
        Location location = new Location(
            request.getLocation().getCourtName(),
            request.getLocation().getCourtAddress(),
            request.getLocation().getLatitude(),
            request.getLocation().getLongitude()
        );
        entry.setLocation(location);
        entry.setMatched(false);
        entry.setIsPrematched(request.isPreMatch());

        if (request.getLocation().getLatitude() != 0 && request.getLocation().getLongitude() != 0 && request.isPreMatch()) {
            entry.setMatchType(MatchQueueType.QUEUE_LOCATION);
        } else {
            entry.setMatchType(MatchQueueType.QUEUE_GYM);
        }

        if (request.isPreMatch()) {
            entry.setDate(request.getDate());
            entry.setTime(request.getTime());
        } else {
            entry.setDate(LocalDate.now());
            entry.setTime(LocalTime.now());
        }

        if (user.getGameRoom() != null) {
            entry.setGameRoom(user.getGameRoom());
        }

        return new MatchQueueResponse(matchQueueRepository.save(entry));
    }
    // 매칭 큐 등록 취소
    public void cancelQueueEntry(Long userId) {
        List<MatchQueueEntry> entries = matchQueueRepository.findByUser_UserIdAndMatchedFalse(userId);
        if (entries.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "매칭 큐에 등록된 유저가 없습니다.");
        }
        matchQueueRepository.deleteAll(entries);
    }

    // 게임방 찾기
    public GameRoom getGameRoomById(Long roomId) {
        return gameRoomRepository.findById(roomId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "게임방을 찾을 수 없습니다."));
    }

    // 사전 수동 매칭 (구장)
    public Game createManualGameFromRoom(GameRoom room, List<Long> userIds, LocalDate date, LocalTime time) {
        validateUsersBeforeMatch(userIds, room);
        List<MatchQueueEntry> entries = matchQueueRepository.findByUser_UserIdInAndMatchedFalse(userIds)
                .stream().filter(e -> e.getMatchType() == MatchQueueType.QUEUE_GYM).toList();

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
        gameRepository.save(game);

        for (NormalUser user : game.getParticipants()) {
            user.setCurrentGame(game);
        }
        normalUserRepository.saveAll(game.getParticipants());
        return game;
    }
    // 현장 매칭 수동(구장)
    public Game createLiveGameFromRoom(GameRoom room, List<Long> userIds) {
        validateUsersBeforeMatch(userIds, room);
        List<MatchQueueEntry> entries = matchQueueRepository.findByUser_UserIdInAndMatchedFalse(userIds)
                .stream().filter(e -> e.getMatchType() == MatchQueueType.QUEUE_GYM).toList();

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
        gameRepository.save(game);

        for (NormalUser user : game.getParticipants()) {
            user.setCurrentGame(game);
        }
        normalUserRepository.saveAll(game.getParticipants());
        return game;
    }

    // 사전매칭 동네를 위한 가까운 거리 유저 리스트 반환
    public List<MatchQueueEntry> getNearbyPreLocationQueue(double latitude, double longitude) {
        List<MatchQueueEntry> queue = matchQueueRepository.findByMatchedFalseAndMatchTypeAndGameRoomIsNull(MatchQueueType.QUEUE_LOCATION);

        return queue.stream()
                .filter(entry -> {
                    Location loc = entry.getLocation();
                    if (loc != null) {
                        double dist = GeoUtil.calculateDistance(latitude, longitude, loc.getLatitude(), loc.getLongitude());
                        System.out.println(">> userId: " + entry.getUser().getUserId() +
                                ", lat: " + loc.getLatitude() + ", lon: " + loc.getLongitude() +
                                ", 거리: " + dist + "m");
                        return dist <= 300;
                    }
                    return false;
                })
                .collect(Collectors.toList());
    }
    // 사전 수동 매칭(동네) - 게임방 생성
    public GameRoom createGameRoomFromUserList(String courtName,
                                               String courtAddress,
                                               LocalDate date,
                                               LocalTime time,
                                               List<Long> userIds) {

        List<NormalUser> users = normalUserRepository.findAllById(userIds);
        if (users.size() != userIds.size()) {
            throw new IllegalArgumentException("일부 유저를 찾을 수 없습니다.");
        }

        validateUsersBeforeMatch(userIds, null);

        Location location = new Location(courtName, courtAddress);

        GameRoom room = new GameRoom();
        room.setLocation(location);
        room.setDate(date);
        room.setTime(time);
        room.setParticipants(users);
        gameRoomRepository.save(room);

        for (NormalUser user : users) {
            user.setGameRoom(room);
        }
        normalUserRepository.saveAll(users);
        return room;
    }
}
