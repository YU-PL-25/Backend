package PL_25.shuttleplay.Service.GameRoom;

import PL_25.shuttleplay.Entity.Game.GameRoom;
import PL_25.shuttleplay.Repository.GameRoomRepository;
import PL_25.shuttleplay.dto.GameRoomPutDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;

@Service
@RequiredArgsConstructor
public class GameRoomPutService {

    private final GameRoomRepository gameRoomRepository;

    /*
        save 성공 => return true
        save 실패 => return false
    */
    @Transactional
    public boolean putGameRoom(GameRoomPutDTO gameRoomPutDTO) {

        try {

            // 프론트에서 가져온 데이터로 GameRoom 엔티티 생성.
            GameRoom gameRoom = GameRoom.builder()
                    .participants(new ArrayList<>())
                    .gameList(new ArrayList<>())
                    .location(gameRoomPutDTO.getLocation())
                    .date(LocalDate.now())
                    .time(LocalTime.now())
                    .build();

            // db에 저장하기.
            gameRoomRepository.save(gameRoom);
            // save() 성공했으면 return true.
            return true;
        } catch (Exception e) {
            // 에러 메시지 출력.
            e.printStackTrace();
            // save()가 실패했으면 return false.
            return false;
        }
    }
}
