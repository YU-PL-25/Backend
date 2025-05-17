package PL_25.shuttleplay.Service;

import PL_25.shuttleplay.Entity.Game.GameRoom;
import PL_25.shuttleplay.Entity.Location;
import PL_25.shuttleplay.Entity.User.NormalUser;
import PL_25.shuttleplay.Repository.GameRoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ManualMatchService {

    private final GameRoomRepository gameRoomRepository;

    public GameRoom createManualMatch(List<NormalUser> selectedUsers, boolean isLive,
                                      String courtName, String courtAddress,
                                      LocalDate date, LocalTime time) {
        GameRoom room = new GameRoom();

        // 위치 설정
        Location location = new Location();
        location.setCourtName(courtName);
        location.setCourtAddress(courtAddress);
        room.setLocation(location);

        // 날짜/시간 설정
        if (isLive) {
            room.setDate(LocalDate.now());
            room.setTime(LocalTime.now());
        } else {
            room.setDate(date);
            room.setTime(time);
        }

        // 참여자 설정
        room.setParticipants(selectedUsers);

        return gameRoomRepository.save(room);
    }
}
