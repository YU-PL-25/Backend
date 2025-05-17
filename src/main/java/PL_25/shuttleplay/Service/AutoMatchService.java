package PL_25.shuttleplay.Service;

import PL_25.shuttleplay.Dto.Matching.AutoMatchRequest;
import PL_25.shuttleplay.Entity.Game.Game;
import PL_25.shuttleplay.Entity.Game.GameRoom;
import PL_25.shuttleplay.Entity.Game.MatchQueueEntry;
import PL_25.shuttleplay.Entity.Location;
import PL_25.shuttleplay.Entity.User.NormalUser;
import PL_25.shuttleplay.Repository.GameRepository;
import PL_25.shuttleplay.Repository.GameRoomRepository;
import PL_25.shuttleplay.Repository.MatchQueueRepository;
import PL_25.shuttleplay.Util.GeoUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AutoMatchService {

    private final MatchQueueRepository matchQueueRepository;
    private final GameRoomRepository gameRoomRepository;
    private final GameRepository gameRepository;

    // ========================== 사전매칭 (구장 기준) ==========================
    public Game matchPreCourtFromRoom(Long roomId) {
        List<MatchQueueEntry> queue = matchQueueRepository.findByMatchedFalseAndGameRoom_GameRoomId(roomId);

        for (MatchQueueEntry me : queue) {
            List<MatchQueueEntry> candidates = queue.stream()
                    .filter(other -> !other.equals(me))
                    .filter(other -> isSameCourt(me, other))
                    .filter(other -> isSimilarEnough(me, other))
                    .sorted((a, b) -> Double.compare(
                            calculateSimilarity(b, me),
                            calculateSimilarity(a, me)
                    ))
                    .limit(getRequiredMatchCount(me.getProfile().getGameType()))
                    .toList();

            if (candidates.size() == getRequiredMatchCount(me.getProfile().getGameType())) {
                List<MatchQueueEntry> matchedGroup = new ArrayList<>(candidates);
                matchedGroup.add(me);

                Game game = buildGameFromQueueEntries(matchedGroup);
                matchedGroup.forEach(entry -> {
                    entry.setMatched(true);
                    entry.setIsPrematched(true);
                });
                matchQueueRepository.saveAll(matchedGroup);

                return gameRepository.save(game);
            }
        }
        return null;
    }

    // ========================== 현장매칭 (구장 기준) ==========================
    public Game matchLiveCourtFromRoom(Long roomId) {
        List<MatchQueueEntry> queue = matchQueueRepository.findByMatchedFalseAndGameRoom_GameRoomId(roomId);

        for (MatchQueueEntry me : queue) {
            List<MatchQueueEntry> candidates = queue.stream()
                    .filter(other -> !other.equals(me))
                    .filter(other -> isSimilarEnough(me, other))
                    .sorted((a, b) -> Double.compare(
                            calculateSimilarity(b, me),
                            calculateSimilarity(a, me)
                    ))
                    .limit(getRequiredMatchCount(me.getProfile().getGameType()))
                    .toList();

            if (candidates.size() == getRequiredMatchCount(me.getProfile().getGameType())) {
                List<MatchQueueEntry> matchedGroup = new ArrayList<>(candidates);
                matchedGroup.add(me);

                Game game = buildGameFromQueueEntries(matchedGroup);
                matchedGroup.forEach(entry -> entry.setMatched(true));
                matchQueueRepository.saveAll(matchedGroup);

                return gameRepository.save(game);
            }
        }
        return null;
    }

    // ========================== 사전매칭 (동네 기준) ==========================
    public GameRoom createPreLocationMeetingRoomFromQueue() {
        List<MatchQueueEntry> queue = matchQueueRepository.findByMatchedFalse();

        for (MatchQueueEntry me : queue) {
            List<MatchQueueEntry> candidates = queue.stream()
                    .filter(other -> !other.equals(me))
                    .filter(other -> isNearby(me.getLocation(), other.getLocation()))
                    .filter(other -> isSimilarEnough(me, other))
                    .sorted((a, b) -> Double.compare(
                            calculateSimilarity(b, me),
                            calculateSimilarity(a, me)
                    ))
                    .limit(getRequiredMatchCount(me.getProfile().getGameType()))
                    .toList();

            if (candidates.size() == getRequiredMatchCount(me.getProfile().getGameType())) {
                List<NormalUser> users = new ArrayList<>();
                candidates.forEach(e -> {
                    e.setMatched(true);
                    users.add(e.getUser());
                });
                me.setMatched(true);
                users.add(me.getUser());

                GameRoom room = new GameRoom();
                room.setDate(me.getDate());
                room.setTime(me.getTime());
                room.setLocation(me.getLocation());
                room.setParticipants(users);

                matchQueueRepository.saveAll(candidates);
                matchQueueRepository.save(me);

                return gameRoomRepository.save(room);
            }
        }
        return null;
    }

    // ========================== 게임 생성 ==========================
    public Game buildGameFromQueueEntries(List<MatchQueueEntry> entries) {
        Game game = new Game();
        game.setParticipants(entries.stream().map(MatchQueueEntry::getUser).toList());

        MatchQueueEntry base = entries.get(0);
        game.setDate(base.getDate());
        game.setTime(base.getTime());
        game.setLocation(base.getLocation());

        return game;
    }

    // ========================== 유사도 계산 ==========================
    private boolean isSameCourt(MatchQueueEntry a, MatchQueueEntry b) {
        return a.getDate().equals(b.getDate()) &&
               a.getTime().equals(b.getTime()) &&
               a.getLocation().getCourtName().equals(b.getLocation().getCourtName());
    }

    private boolean isNearby(Location a, Location b) {
        return GeoUtil.calculateDistance(a.getLatitude(), a.getLongitude(),
                                         b.getLatitude(), b.getLongitude()) <= 300;
    }

    private boolean isSimilarEnough(MatchQueueEntry a, MatchQueueEntry b) {
        return calculateSimilarity(a, b) >= 0.5;
    }

    private double calculateSimilarity(MatchQueueEntry a, MatchQueueEntry b) {
        int match = 0;
        if (a.getProfile().getAgeGroup().equals(b.getProfile().getAgeGroup())) match++;
        if (a.getProfile().getGameType().equals(b.getProfile().getGameType())) match++;
        if (a.getProfile().getPlayStyle().equals(b.getProfile().getPlayStyle())) match++;
        if (Math.abs(a.getMmr().getRating() - b.getMmr().getRating()) <= a.getMmr().getTolerance()) match++;

        return match / 4.0;
    }

    private int getRequiredMatchCount(String gameType) {
        return switch (gameType) {
            case "단식" -> 1;
            case "혼합복식", "남자복식", "여자복식" -> 3;
            default -> throw new IllegalArgumentException("지원하지 않는 게임 타입: " + gameType);
        };
    }
}
