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
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class GameRoomService {

    private final LocationService locationService;
    private final GameRoomRepository gameRoomRepository;
    private final NormalUserRepository normalUserRepository;



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
        master.setGameRoom(gameRoom);
        master.setRole("manager"); // 방장 권한 부여
        normalUserRepository.save(master);

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

        // 둘다 있으면 유저의 GameRoom 설정.
        user.setGameRoom(gameRoom);

        return gameRoom;
    }
}
