package PL_25.shuttleplay.Service;

import PL_25.shuttleplay.dto.Matching.ManualMatchRequest;
import PL_25.shuttleplay.Entity.Game.*;
import PL_25.shuttleplay.Repository.*;
import PL_25.shuttleplay.dto.Matching.ManualMatchRequest;
import PL_25.shuttleplay.Entity.Location;
import PL_25.shuttleplay.Entity.User.NormalUser;
import PL_25.shuttleplay.Util.GeoUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ManualMatchService {

    private final GameRoomRepository gameRoomRepository;
    private final GameRepository gameRepository;
    private final GameHistoryRepository gameHistoryRepository;
    private final MatchQueueRepository matchQueueRepository;
    private final NormalUserRepository normalUserRepository;

    public void validateUsersBeforeMatch(List<Long> userIds, GameRoom currentRoomOrNull) {
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

        if (request.isPreMatch()) {
            entry.setMatchType(MatchQueueType.QUEUE_PRE);
            entry.setDate(request.getDate());
            entry.setTime(request.getTime());
        } else {
            entry.setMatchType(MatchQueueType.QUEUE_LIVE);
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
                .stream().filter(e -> e.getMatchType() == MatchQueueType.QUEUE_LIVE).toList();

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
        Game savedGame = gameRepository.save(game);

        // 게임 히스토리 생성
        GameHistory history = new GameHistory();
        history.setGame(savedGame);
        history.setScoreTeamA(0);
        history.setScoreTeamB(0);
        history.setCompleted(false);
        gameHistoryRepository.save(history);

        for (NormalUser user : savedGame.getParticipants()) {
            user.setCurrentGame(savedGame);
        }
        normalUserRepository.saveAll(savedGame.getParticipants());

        return savedGame;
    }

    // 현장 매칭 수동(구장)
    public Game createLiveGameFromRoom(GameRoom room, List<Long> userIds) {
        validateUsersBeforeMatch(userIds, room);

        List<MatchQueueEntry> entries = matchQueueRepository.findByUser_UserIdInAndMatchedFalse(userIds)
                .stream().filter(e -> e.getMatchType() == MatchQueueType.QUEUE_LIVE).toList();

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
        Game savedGame = gameRepository.save(game);

        // 게임 히스토리 생성
        GameHistory history = new GameHistory();
        history.setGame(savedGame);
        history.setScoreTeamA(0);
        history.setScoreTeamB(0);
        history.setCompleted(false);
        gameHistoryRepository.save(history);

        for (NormalUser user : savedGame.getParticipants()) {
            user.setCurrentGame(savedGame);
        }
        normalUserRepository.saveAll(savedGame.getParticipants());

        return savedGame;
    }

    // 사전 수동 매칭(동네 기반) - 사용자가 입력한 위치/날짜/시간을 기준으로
    // 300m 이내에 존재하는 다른 게임방 목록을 조회하는 메서드
    public List<GameRoom> findNearbyRooms(double latitude, double longitude, LocalDate date, LocalTime time) {
        return gameRoomRepository.findByDateAndTime(date, time)  // 같은 날짜 & 시간대의 모든 게임방 조회
                .stream()
                .filter(room -> {
                    Location loc = room.getLocation(); // 방의 위치 정보
                    // 유저의 현재 위치와 방의 위치 간 거리 계산
                    double distance = GeoUtil.calculateDistance(latitude, longitude, loc.getLatitude(), loc.getLongitude());
                    return distance <= 300; //  300m 이내인 방만 필터링
                })
                .toList(); // 최종 리스트 반환
    }

    // 사전 수동 매칭(동네) - 혼자 게임방 생성 (다른 유저가 참여하길 기다리는 방)
    public GameRoom createGameRoomForOneUser(String courtName,
                                             String courtAddress,
                                             double latitude,
                                             double longitude,
                                             LocalDate date,
                                             LocalTime time,
                                             Long userId) {

        // 사용자 조회
        NormalUser user = normalUserRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("유저를 찾을 수 없습니다."));

        // 현재 진행 중인 게임이나 다른 방 참여 여부 검사
        validateUsersBeforeMatch(List.of(userId), null);

        // 위치 객체 생성 (좌표 포함)
        Location location = new Location(courtName, courtAddress, latitude, longitude);

        // 게임방 생성 및 사용자 등록
        GameRoom room = new GameRoom();
        room.setLocation(location);
        room.setDate(date);
        room.setTime(time);
        room.setParticipants(List.of(user));  // 생성자는 자동 등록

        // DB 저장
        GameRoom savedRoom = gameRoomRepository.save(room);

        // 사용자-게임방 연결
        user.setGameRoom(savedRoom);
        normalUserRepository.save(user);

        return savedRoom;
    }
}
