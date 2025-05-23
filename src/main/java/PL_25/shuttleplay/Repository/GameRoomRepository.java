package PL_25.shuttleplay.Repository;

import PL_25.shuttleplay.Entity.Game.GameRoom;
import PL_25.shuttleplay.Entity.Location;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface GameRoomRepository extends JpaRepository<GameRoom, Long> {
    // 사전매칭 시 날짜, 시간, 장소로 게임방 조회
    List<GameRoom> findByLocation_CourtNameAndDateAndTime(String courtName, LocalDate date, LocalTime time);
}
