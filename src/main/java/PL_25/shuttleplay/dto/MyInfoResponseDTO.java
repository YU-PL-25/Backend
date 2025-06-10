package PL_25.shuttleplay.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class MyInfoResponseDTO {
    private Long userId;
    private String name;
    private String nickname;
    private String email;
    private String phone;
    private String gender;
    private String role;

    private String rank;
    private int rating;
    private int gamesPlayed;
    private int winsCount;
    private double winRate;

    private String ageGroup;
    private String playStyle;
    private String gameType;
}