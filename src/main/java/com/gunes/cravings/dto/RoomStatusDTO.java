package com.gunes.cravings.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomStatusDTO {
    private String roomCode;
    private List<String> usersInRoom; // Odadaki kullanıcı e-postalarının listesi
    private String teamACaptainEmail;
    private String teamBCaptainEmail;
}