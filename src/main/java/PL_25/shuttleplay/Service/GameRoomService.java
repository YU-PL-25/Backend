package PL_25.shuttleplay.Service;

import PL_25.shuttleplay.dto.Matching.CurrentMatchingGameRoomDTO;
import PL_25.shuttleplay.dto.Matching.PreMatchingGameRoomDTO;
import PL_25.shuttleplay.Entity.Game.GameRoom;
import PL_25.shuttleplay.Entity.Location;
import PL_25.shuttleplay.Entity.User.NormalUser;
import PL_25.shuttleplay.Repository.GameRoomRepository;
import PL_25.shuttleplay.Repository.NormalUserRepository;
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



    // 현장 매칭(구장) // 방장pk와 구장 위치 받음.
    @Transactional
    public GameRoom putCurrentMatchingGameRoom(CurrentMatchingGameRoomDTO gameRoomDTO) {


        // pk로 방장 NormalUser 가져오기
        NormalUser master = normalUserRepository
                .findById(gameRoomDTO.getMasterId()).orElse(null);
        // 방장이 없으면 return null;
        if (master == null) {
            return null;
        }

        // Location이 db에 있으면 새로 만들고 반환, 이미 있으면 그거 반환.
        Location selectedLocation = locationService.findOrCreateLocation(gameRoomDTO.getLocation());

        // 프론트에서 가져온 데이터로 GameRoom 엔티티 생성.
        GameRoom gameRoom = GameRoom.builder()
                .location(selectedLocation)
                .date(LocalDate.now())
                .time(LocalTime.now())
                .build();

        // 방장의 GameRoom 설정.
        master.setGameRoom(gameRoom);

        // db에 저장하기.
        return gameRoomRepository.save(gameRoom);
    }


    // 사전 매칭(구장) // 방장pk, 위치, 날짜, 시간 받음.
    @Transactional
    public GameRoom putPreMatchingGameRoom(PreMatchingGameRoomDTO gameRoomDTO) {

        // Location이 db에 있으면 새로 만들고 있으면 반환. (여기선 무시)
        locationService.findOrCreateLocation(gameRoomDTO.getLocation());

        // pk로 방장 NormalUser 가져오기
        NormalUser master = normalUserRepository
                .findById(gameRoomDTO.getMasterId()).orElse(null);

        // 방장이 없으면 return null;
        if (master == null) {
            return null;
        }

        // Location이 db에 있으면 새로 만들고 반환, 이미 있으면 그거 반환.
        Location selectedLocation = locationService.findOrCreateLocation(gameRoomDTO.getLocation());

        // 프론트에서 가져온 데이터로 GameRoom 엔티티 생성.
        GameRoom gameRoom = GameRoom.builder()
                .location(selectedLocation)
                .date(gameRoomDTO.getLocalDate())
                .time(gameRoomDTO.getLocalTime())
                .build();

        // 방장의 GameRoom과 참가자 설정.
        master.setGameRoom(gameRoom);
        gameRoom.getParticipants().add(master);

        // db에 저장하기.
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

        return gameRoom;
    }


    // 게임방 삭제.
    @Transactional
    public void deleteGameRoom(long gameRoomId) {

        GameRoom gameRoom = gameRoomRepository.findById(gameRoomId)
                .orElseThrow(() -> new NoSuchElementException("해당 게임방 없음"));

        gameRoomRepository.delete(gameRoom);
    }


    @Transactional(readOnly = true)
    public List<GameRoom> selectAllGameRoom() {

        return gameRoomRepository.findAll();
    }
}
