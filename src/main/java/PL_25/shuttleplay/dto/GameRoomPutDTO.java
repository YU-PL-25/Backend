package PL_25.shuttleplay.dto;

import PL_25.shuttleplay.Entity.Game.Game;
import PL_25.shuttleplay.Entity.Location;
import PL_25.shuttleplay.Entity.User.NormalUser;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GameRoomPutDTO {

//    private List<NormalUser> participants;
//    private List<Game> gameList;
    private Location location;
}
