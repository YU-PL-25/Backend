package PL_25.shuttleplay.Service;

import PL_25.shuttleplay.Dto.Matching.AutoMatchRequest;
import PL_25.shuttleplay.Entity.Game.GameRoom;
import PL_25.shuttleplay.Entity.Location;
import PL_25.shuttleplay.Entity.User.NormalUser;
import PL_25.shuttleplay.Repository.GameRoomRepository;
import PL_25.shuttleplay.Util.GeoUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

// 자동 매칭 서비스
@Service
@RequiredArgsConstructor
public class AutoMatchService {

    private final GameRoomRepository gameRoomRepository;

    // 사전매칭(구장 기준) -> 날짜/시간/구장명 동일 + 유사도 비교
    public List<AutoMatchRequest> matchPreCourt(AutoMatchRequest me, List<AutoMatchRequest> pool){
        return pool.stream()
                .filter(other -> !other.equals(me))
                .filter(other -> isSameCourt(me, other))
                .sorted((a, b) -> Double.compare(
                    calculateSimilarity(b, me),
                    calculateSimilarity(a, me)
                ))
                .limit(getRequiredMatchCount(me.getProfile().getGameType()))
                .toList();

    }

    // 사전매칭 (동네 기준) → 위치 반경 300m 이내 + 유사도 비교
    public GameRoom createPreLocationMeetingRoom(AutoMatchRequest me, List<AutoMatchRequest> pool) {
        List<AutoMatchRequest> matched = pool.stream()
            .filter(other -> !other.equals(me))
            .filter(other -> isNearby(me.getLocation(), other.getLocation()))
            .filter(other -> isSimilarEnough(me, other))
            .sorted((a, b) -> Double.compare(
                calculateSimilarity(b, me),
                calculateSimilarity(a, me)
            ))
            .limit(getRequiredMatchCount(me.getProfile().getGameType()))
            .toList();

        List<NormalUser> participants = matched.stream()
            .map(AutoMatchRequest::getUser) // MatchRequest에 user 필드 필요
            .toList();

        GameRoom room = new GameRoom();
        room.setDate(me.getDate());
        room.setTime(me.getTime());
        room.setLocation(me.getLocation());
        room.setParticipants(participants);

        return gameRoomRepository.save(room);
    }

    // 현장매칭 (구장 기준) → 거리/시간/위치 안봄 + 유사도만 비교
    public List<AutoMatchRequest> matchLiveCourt(AutoMatchRequest me, List<AutoMatchRequest> pool){
        return pool.stream()
                .filter(other -> !other.equals(me))
                .sorted((a, b) -> Double.compare(
                    calculateSimilarity(b, me),
                    calculateSimilarity(a, me)
                ))
                .limit(getRequiredMatchCount(me.getProfile().getGameType()))
                .toList();
    }

    // 구장 기준 비교 (날짜+시간+구장명 동일)
    private boolean isSameCourt(AutoMatchRequest a, AutoMatchRequest b) {
        return a.getDate().equals(b.getDate()) &&
               a.getTime().equals(b.getTime()) &&
               a.getLocation().getCourtName().equals(b.getLocation().getCourtName());
    }

    // 동네 기준 거리 비교(300m 이내)
    private boolean isNearby(Location a, Location b) {
        return GeoUtil.calculateDistance(
                a.getLatitude(), a.getLongitude(),
                b.getLatitude(), b.getLongitude()
        ) <= 300;
    }

    // 유사도 계산(60% 이상만 통과)
    private boolean isSimilarEnough(AutoMatchRequest a, AutoMatchRequest b) {
        return calculateSimilarity(a, b) >= 0.6;
    }

    public double calculateSimilarity(AutoMatchRequest a, AutoMatchRequest b) {
        int matchCount = 0;

        if(a.getProfile().getAgeGroup().equals(b.getProfile().getAgeGroup())) matchCount++;
        if(a.getProfile().getGameType().equals(b.getProfile().getGameType())) matchCount++;
        if(a.getProfile().getPlayStyle().equals(b.getProfile().getPlayStyle())) matchCount++;

        int mmrA = a.getMmr().getRating();
        int mmrB = b.getMmr().getRating();
        int tolerance = a.getMmr().getTolerance();

        if(Math.abs(mmrA - mmrB) <= tolerance) matchCount++;

        return matchCount / 4.0;
    }

    // 게임타입에 따라 필요한 매칭 인원 수 반환
    private int getRequiredMatchCount(String gameType) {
        return switch (gameType) {
            case "단식" -> 1; // 본인 제외 1명
            case "혼합복식", "남자복식", "여자복식" -> 3; // 본인 제외 3명
            default -> throw new IllegalArgumentException("지원하지 않는 게임 타입: " + gameType);
        };
    }
}
