package PL_25.shuttleplay.Repository;

import PL_25.shuttleplay.Entity.Game.GameRoom;
import PL_25.shuttleplay.Entity.Location;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface GameRoomRepository extends JpaRepository<GameRoom, Long> {
    // 사전매칭 시 날짜, 시간으로 게임방 조회
    List<GameRoom> findByDateAndTime(LocalDate date, LocalTime time);

    // Location으로 GameRoom 리스트 조회
    List<GameRoom> findByLocation(Location location);
}
