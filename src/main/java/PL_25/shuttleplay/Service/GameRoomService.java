package PL_25.shuttleplay.Service;

import PL_25.shuttleplay.dto.Matching.*;

import PL_25.shuttleplay.Entity.Game.GameRoom;
import PL_25.shuttleplay.Entity.Location;
import PL_25.shuttleplay.Entity.Game.MatchQueueEntry;
import PL_25.shuttleplay.Entity.User.NormalUser;

import PL_25.shuttleplay.Repository.GameRoomRepository;
import PL_25.shuttleplay.Repository.NormalUserRepository;
import PL_25.shuttleplay.Repository.LocationRepository;
import PL_25.shuttleplay.Repository.MatchQueueRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class GameRoomService {

    private final LocationService locationService;
    private final GameRoomRepository gameRoomRepository;
    private final NormalUserRepository normalUserRepository;
    private final LocationRepository locationRepository;
    private final MatchQueueRepository matchQueueRepository;



    // 현장 매칭(구장) // 방장pk와 구장 위치 받음.
    @Transactional
    public GameRoom putCurrentMatchingGameRoom(CurrentMatchingGameRoomDTO gameRoomDTO) {

        // 1. 방장 user 조회
        // pk로 방장 NormalUser 가져오기
        NormalUser master = normalUserRepository
                .findById(gameRoomDTO.getMasterId()).orElse(null);
        // 방장이 없으면 return null;
        if (master == null) {
            return null;
        }

        // 2. 위치 정보 생성 또는 재사용
        // Location이 db에 있으면 새로 만들고 반환, 이미 있으면 그거 반환.
        Location selectedLocation = locationService.findOrCreateLocation(gameRoomDTO.getLocation());

        // 3. GameRoom 생성 (createdBy = master)
        // 프론트에서 가져온 데이터로 GameRoom 엔티티 생성.
        GameRoom gameRoom = GameRoom.builder()
                .title(gameRoomDTO.getTitle())      // 방제 저장
                .location(selectedLocation)
                .date(LocalDate.now())
                .time(LocalTime.now())
                .createdBy(master) // 방장 저장
                .build();

        // 4. 방장의 게임방 등록 및 role 지정
        master.setGameRoom(gameRoom);
        master.setRole("manager"); // 방장 권한 부여
        normalUserRepository.save(master);

        // 5. db에 게임방 저장
        return gameRoomRepository.save(gameRoom);
    }


    // 사전 매칭(구장) // 방장pk, 위치, 날짜, 시간 받음.
    @Transactional
    public GameRoom putPreMatchingGameRoom(PreMatchingGameRoomDTO gameRoomDTO) {

        // Location이 db에 있으면 새로 만들고 있으면 반환. (여기선 무시)
        locationService.findOrCreateLocation(gameRoomDTO.getLocation());

        // 1. 방장 user 조회
        // pk로 방장 NormalUser 가져오기
        NormalUser master = normalUserRepository
                .findById(gameRoomDTO.getMasterId()).orElse(null);

        // 방장이 없으면 return null;
        if (master == null) {
            return null;
        }

        // 2. 위치 정보 생성 또는 재사용
        // Location이 db에 있으면 새로 만들고 반환, 이미 있으면 그거 반환.
        Location selectedLocation = locationService.findOrCreateLocation(gameRoomDTO.getLocation());

        // 3. GameRoom 생성 (createdBy = master)
        // 프론트에서 가져온 데이터로 GameRoom 엔티티 생성.
        GameRoom gameRoom = GameRoom.builder()
                .location(selectedLocation)
                .date(gameRoomDTO.getLocalDate())
                .time(gameRoomDTO.getLocalTime())
                .createdBy(master) // 방장 저장
                .build();

        // 4. 방장의 게임방 등록 및 role 지정
        // 방장의 GameRoom과 참가자 설정.
        master.setGameRoom(gameRoom);
        master.setRole("manager"); // 방장 권한 부여
        normalUserRepository.save(master);
        gameRoom.getParticipants().add(master);

        // 5. db에 게임방 저장
        return gameRoomRepository.save(gameRoom);
    }


    // 참가 요청한 유저를 특정 GameRoom에 넣기
    @Transactional
    public GameRoom addUserToGameRoom(long gameRoomId, long userId) {

        // GameRoom이 없으면 throw.
        GameRoom gameRoom = gameRoomRepository.findById(gameRoomId)
                .orElseThrow(() -> new NoSuchElementException("해당 게임방 없음 : " + gameRoomId));

        // 요청한 NormalUser가 없으면 throw.
        NormalUser user = normalUserRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("요청한 유저 없음 : " + userId));

        // 둘다 있으면 GameRoom과 participants 설정
        user.setGameRoom(gameRoom);
        gameRoom.getParticipants().add(user);

        return gameRoom;
    }


    // 유저가 참가한 방 나가기.
    @Transactional
    public GameRoom leaveGameRoom(long userId) {

        // 해당 user가 없으면 throw.
        NormalUser user = normalUserRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("해당 유저 없음 : " + userId));

        // 해당 유저가 게임방에 참가하고 있지 않으면 throw.
        GameRoom gameRoom = Optional.ofNullable(user.getGameRoom())
                .orElseThrow(() -> new NoSuchElementException("해당 유저는 참가중인 게임방이 없음 : " + userId));

        // 해당 게임방이 실제로 db에 있는지 확인.
        gameRoomRepository.findById(gameRoom.getGameRoomId())
                .orElseThrow(() -> new NoSuchElementException("해당 게임방은 없음"));

        // 유저가 참가한 게임방을 없애기.
        user.setGameRoom(null);
        gameRoom.getParticipants().remove(user);

        // 게임방 나갈 때 해당 유저를 대기열에서 삭제.
        List<MatchQueueEntry> entries = matchQueueRepository.findByUser_UserIdAndMatchedFalse(userId);
        if (!entries.isEmpty()) {
            matchQueueRepository.deleteAll(entries);
        }

        return gameRoom;
    }


    // 게임방 삭제.
    @Transactional
    public void deleteGameRoom(long gameRoomId) {

        GameRoom gameRoom = gameRoomRepository.findById(gameRoomId)
                .orElseThrow(() -> new NoSuchElementException("해당 게임방 없음"));

        // GameRoom의 참조를 NormalUser가 가지고 있어서 null로 처리.
        for (NormalUser n : gameRoom.getParticipants()) {
            n.setGameRoom(null);
        }

        // 게임방 삭제할 때 해당 게임방을 가지는 모든 유저 삭제.
        List<MatchQueueEntry> entries = matchQueueRepository.findByGameRoom_GameRoomIdAndMatchedFalse(gameRoomId);
        if (!entries.isEmpty()) {
            matchQueueRepository.deleteAll(entries);
        }

        // 게임방 자체 삭제.
        gameRoomRepository.delete(gameRoom);
    }


    // 게임방 전체 조회 - dto로 반환하도록 수정
    @Transactional(readOnly = true)
    public List<GameRoomDTO> selectAllGameRoom() {
        List<GameRoom> rooms = gameRoomRepository.findAll();
        return rooms.stream().map(room -> {
            // 참가자 수 조회
            int participantCount = normalUserRepository.countByGameRoom_GameRoomId(room.getGameRoomId());

            List<GameDTO> gameDTOList = room.getGameList().stream().map(game -> {
                List<PlayerDTO> players = game.getParticipants().stream().map(p -> {
                    var user = p.getUser();
                    return PlayerDTO.builder()
                            .userId(user.getUserId())
                            .nickname(user.getNickname())
                            .rank(user.getRank().name())
//                            .team(p.getTeam() != null ? p.getTeam().name() : null)
                            .build();
                }).toList();

                return GameDTO.builder()
                        .gameId(game.getGameId())
                        .status(game.getStatus().name())
                        .matchType(game.getMatchType())
                        .date(game.getDate().toString())
                        .time(game.getTime().toString())
                        .players(players)
                        .build();
            }).toList();

            return GameRoomDTO.builder()
                    .gameRoomId(room.getGameRoomId())
                    .managerId(room.getCreatedBy().getUserId())
                    .title(room.getTitle())
                    .locationName(room.getLocation().getCourtName())
                    .locationAddress(room.getLocation().getCourtAddress())
                    .games(gameDTOList)
                    .participantCount(participantCount)
                    .build();
        }).toList();
    }



    // 같은 구장에 있는 게임방 조회
    @Transactional(readOnly = true)
    public List<GameRoom> selectGameRoomByLocation(long locationId) {

        Location location = locationRepository.findById(locationId)
                .orElseThrow(() -> new NoSuchElementException("해당 위치는 존재하지 않습니다 : " + locationId));

        return gameRoomRepository.findByLocation(location);
    }


    @Transactional(readOnly = true)
    public GameRoom selectGameRoomById(long gameRoomId) {

        return gameRoomRepository.findById(gameRoomId)
                .orElseThrow(() -> new NoSuchElementException("해당 id를 가지는 게임방은 없습니다. : " + gameRoomId));
    }

  
    // 하나의 GameRoom에 속해있는 Game들을 조회
    // GameRoomDTO - 해당 gameRoomId를 가진 GameRoom에 대한 정보 제공
    // GameDTO - 해당 gameRoomId에 속해있는 Game들에 대한 정보 제공
    @Transactional(readOnly = true)
    public GameRoomDTO getGamesByGameRoomId(Long gameRoomId) {
        GameRoom gameRoom = gameRoomRepository.findById(gameRoomId)
                .orElseThrow(() -> new NoSuchElementException("게임방이 존재하지 않습니다."));

        List<GameDTO> gameDTOList = gameRoom.getGameList().stream()
                .map(game -> {
                    List<PlayerDTO> players = game.getParticipants().stream()
                            .map(p -> {
                                var user = p.getUser();
                                return PlayerDTO.builder()
                                        .userId(user.getUserId())
                                        .nickname(user.getNickname())
                                        .rank(user.getRank().name())
                                        .team(p.getTeam() != null ? p.getTeam().name() : null)
                                        .build();
                            })
                            .toList();

                    return GameDTO.builder()
                            .gameId(game.getGameId())
                            .status(game.getStatus().name())
                            .matchType(game.getMatchType())
                            .date(game.getDate().toString())
                            .time(game.getTime().toString())
                            .players(players)
                            .build();
                }).toList();


        return GameRoomDTO.builder()
                .gameRoomId(gameRoom.getGameRoomId())
                .managerId(gameRoom.getCreatedBy().getUserId())
                .title(gameRoom.getTitle())
                .locationName(gameRoom.getLocation().getCourtName())
                .locationAddress(gameRoom.getLocation().getCourtAddress())
                .games(gameDTOList)
                .build();
    }

}
