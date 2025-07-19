package com.gunes.cravings.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gunes.cravings.dto.ChatMessageDTO;
import com.gunes.cravings.dto.PlayerDTO;
import com.gunes.cravings.dto.PlayerSelectDTO;
import com.gunes.cravings.dto.RoomStatusDTO;
import com.gunes.cravings.dto.SetCaptainsRequestDTO;
import com.gunes.cravings.dto.StartSelectionRequestDTO;
import com.gunes.cravings.model.Player;
import com.gunes.cravings.repository.PlayerRepository;
import com.gunes.cravings.service.PlayerService;

import jakarta.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;


import java.io.IOException;
import java.security.Principal;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Controller
public class MessageController {
    private static final Logger logger = LoggerFactory.getLogger(MessageController.class);

    private final SimpMessageSendingOperations messagingTemplate;
    private final PlayerService playerService;
    private final PlayerRepository playerRepository;

    // Room state management (in-memory, for simplicity)
    private final Map<String, Set<String>> roomMembers = new ConcurrentHashMap<>();
    private final Map<String, String> roomCodeOwnerMap = new ConcurrentHashMap<>();
    private final Map<String, String> teamACaptains = new ConcurrentHashMap<>();
    private final Map<String, String> teamBCaptains = new ConcurrentHashMap<>();

    // Team Selection State
    private final Map<String, Boolean> selectionInProgressMap = new ConcurrentHashMap<>();
    private final Map<String, List<PlayerDTO>> availablePlayersForSelectionMap = new ConcurrentHashMap<>();
    private final Map<String, Map<String, PlayerDTO>> teamASelectedPlayersMap = new ConcurrentHashMap<>();
    private final Map<String, Map<String, PlayerDTO>> teamBSelectedPlayersMap = new ConcurrentHashMap<>();
    private final Map<String, String> currentPlayerSelectionTurnMap = new ConcurrentHashMap<>();
    private final Map<String, Integer> currentSelectionRoundMap = new ConcurrentHashMap<>(); // To track A-B-A-B turns

    @Autowired
    public MessageController(SimpMessageSendingOperations messagingTemplate, PlayerService playerService, PlayerRepository playerRepository) {
        this.messagingTemplate = messagingTemplate;
        this.playerService = playerService;
        this.playerRepository = playerRepository;
    }

    @MessageMapping("/chat/createRoom")
    public void createRoom(Principal principal) {
        String userEmail = principal.getName();
        String existingRoom = roomMembers.entrySet().stream()
                .filter(entry -> entry.getValue().contains(userEmail))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);

        if (existingRoom != null) {
            messagingTemplate.convertAndSendToUser(userEmail, "/queue/room-join-error", "Zaten bir odadasınız: " + existingRoom);
            logger.warn("User {} tried to create a room but is already in room {}", userEmail, existingRoom);
            return;
        }

        // Check if user has ADMIN role
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ADMIN"));

        if (!isAdmin) {
            messagingTemplate.convertAndSendToUser(userEmail, "/queue/room-join-error", "Oda oluşturma yetkiniz bulunmamaktadır. Lütfen yöneticinizden bir oda kodu alın.");
            logger.warn("User {} tried to create a room without ADMIN role.", userEmail);
            return;
        }


        String roomCode = generateUniqueRoomCode();
        roomMembers.put(roomCode, Collections.synchronizedSet(new HashSet<>()));
        roomCodeOwnerMap.put(roomCode, userEmail); // Owner for room cleanup or specific actions

        roomMembers.get(roomCode).add(userEmail);
        logger.info("User {} added to room {}", userEmail, roomCode);

        messagingTemplate.convertAndSendToUser(userEmail, "/queue/room-created", roomCode);
        messagingTemplate.convertAndSendToUser(userEmail, "/queue/room-joined", roomCode); // Confirm join for the creator

        ChatMessageDTO joinMessage = new ChatMessageDTO("Sistem", userEmail.split("@")[0] + " adlı kullanıcı odaya katıldı.");
        messagingTemplate.convertAndSend("/topic/chat/" + roomCode, joinMessage);
        logger.info("User {} created and joined room: {}", userEmail, roomCode);

        broadcastRoomStatus(roomCode);
    }

    @MessageMapping("/chat/joinRoom/{roomCode}")
    public void joinRoom(@DestinationVariable String roomCode, Principal principal) {
        String userEmail = principal.getName();

        String existingRoom = roomMembers.entrySet().stream()
                .filter(entry -> entry.getValue().contains(userEmail))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);

        if (existingRoom != null) {
            if (existingRoom.equals(roomCode)) {
                // User is already in this room, just confirm.
                messagingTemplate.convertAndSendToUser(userEmail, "/queue/room-joined", roomCode);
                ChatMessageDTO welcomeBackMessage = new ChatMessageDTO("Sistem", userEmail.split("@")[0] + " adlı kullanıcı odaya yeniden katıldı.");
                messagingTemplate.convertAndSend("/topic/chat/" + roomCode, welcomeBackMessage);
                broadcastRoomStatus(roomCode);
                logger.info("User {} is already in room {}, confirming join.", userEmail, roomCode);
            } else {
                messagingTemplate.convertAndSendToUser(userEmail, "/queue/room-join-error", "Zaten bir odadasınız: " + existingRoom + ". Başka bir odaya katılmak için önce mevcut bağlantınızı kesin.");
                logger.warn("User {} tried to join room {} but is already in room {}", userEmail, roomCode, existingRoom);
            }
            return;
        }

        if (!roomMembers.containsKey(roomCode)) {
            messagingTemplate.convertAndSendToUser(userEmail, "/queue/room-join-error", "Oda kodu geçersiz veya mevcut değil: " + roomCode);
            logger.warn("Join error: Room code is not active: {} Requested by: {}", roomCode, userEmail);
            return;
        }

        roomMembers.get(roomCode).add(userEmail);
        logger.info("User {} joined room: {}", userEmail, roomCode);

        messagingTemplate.convertAndSendToUser(userEmail, "/queue/room-joined", roomCode);
        ChatMessageDTO joinMessage = new ChatMessageDTO("Sistem", userEmail.split("@")[0] + " adlı kullanıcı odaya katıldı.");
        messagingTemplate.convertAndSend("/topic/chat/" + roomCode, joinMessage);

        broadcastRoomStatus(roomCode);
    }

    @MessageMapping("/chat/sendMessage/{roomCode}")
    public ChatMessageDTO sendMessage(@DestinationVariable String roomCode, @Payload @Valid ChatMessageDTO chatMessage, Principal principal) {
        String senderEmail = principal.getName();

        if (!roomMembers.containsKey(roomCode) || !roomMembers.get(roomCode).contains(senderEmail)) {
            logger.error("Invalid or inactive room code or user not in room: {} Sender: {}", roomCode, senderEmail);
            messagingTemplate.convertAndSendToUser(senderEmail, "/queue/chat-error", "Geçersiz oda veya odada değilsiniz.");
            return null;
        }

        chatMessage.setSender(senderEmail);
        messagingTemplate.convertAndSend("/topic/chat/" + roomCode, chatMessage);
        // logger.info("Message sent to room {}: From {} - {}", roomCode, senderEmail, chatMessage.getContent());
        return chatMessage;
    }

    @MessageMapping("/chat/setCaptains/{roomCode}")
    public void setCaptains(@DestinationVariable String roomCode, @Payload SetCaptainsRequestDTO request, Principal principal) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            messagingTemplate.convertAndSendToUser(principal.getName(), "/queue/chat-error", "Kimlik doğrulama bilgisi bulunamadı veya oturum süresi dolmuş. Lütfen tekrar giriş yapın.");
            logger.warn("Authentication object is null or not authenticated for user {}. Cannot set captains.", principal != null ? principal.getName() : "unknown");
            return;
        }

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ADMIN"));

        if (!isAdmin) {
            messagingTemplate.convertAndSendToUser(principal.getName(), "/queue/chat-error", "Bu işlemi yapmak için yönetici yetkiniz bulunmamaktadır.");
            logger.warn("User {} tried to set captains in room {} without ADMIN role.", principal.getName(), roomCode);
            return;
        }

        if (!roomMembers.containsKey(roomCode) || !roomMembers.get(roomCode).contains(principal.getName())) {
            messagingTemplate.convertAndSendToUser(principal.getName(), "/queue/chat-error", "Geçersiz oda veya odada değilsiniz.");
            logger.warn("User {} tried to set captains in room {} but is not a member.", principal.getName(), roomCode);
            return;
        }

        Set<String> currentMembers = roomMembers.get(roomCode);
        if (request.getTeamACaptainEmail() != null && !currentMembers.contains(request.getTeamACaptainEmail())) {
            messagingTemplate.convertAndSendToUser(principal.getName(), "/queue/chat-error", "Takım A Kaptanı odada bulunmuyor.");
            logger.warn("Selected Team A Captain {} is not in room {}.", request.getTeamACaptainEmail(), roomCode);
            return;
        }
        if (request.getTeamBCaptainEmail() != null && !currentMembers.contains(request.getTeamBCaptainEmail())) {
            messagingTemplate.convertAndSendToUser(principal.getName(), "/queue/chat-error", "Takım B Kaptanı odada bulunmuyor.");
            logger.warn("Selected Team B Captain {} is not in room {}.", request.getTeamBCaptainEmail(), roomCode);
            return;
        }

        teamACaptains.put(roomCode, request.getTeamACaptainEmail());
        teamBCaptains.put(roomCode, request.getTeamBCaptainEmail());
        logger.info("Captains set for room {}: Team A: {}, Team B: {}", roomCode, request.getTeamACaptainEmail(), request.getTeamBCaptainEmail());

        
        String teamACaptainName = request.getTeamACaptainEmail() != null ? request.getTeamACaptainEmail().split("@")[0] : "Belirlenmedi";
        String teamBCaptainName = request.getTeamBCaptainEmail() != null ? request.getTeamBCaptainEmail().split("@")[0] : "Belirlenmedi";
        ChatMessageDTO captainMessage = new ChatMessageDTO(
                "Sistem",
                String.format("Kaptanlar belirlendi. Takım A: %s, Takım B: %s", teamACaptainName, teamBCaptainName)
        );
        messagingTemplate.convertAndSend("/topic/chat/" + roomCode, captainMessage);
        broadcastRoomStatus(roomCode);
    }

    @MessageMapping("/chat/requestRoomStatus/{roomCode}")
    public void requestRoomStatus(@DestinationVariable String roomCode, Principal principal) {
        String userEmail = principal.getName();
        if (roomMembers.containsKey(roomCode) && roomMembers.get(roomCode).contains(userEmail)) {
            logger.info("User {} requested room status for room {}", userEmail, roomCode);
            sendRoomStatusToUser(roomCode, userEmail);
        } else {
            messagingTemplate.convertAndSendToUser(userEmail, "/queue/chat-error", "Geçersiz oda veya odada değilsiniz. Durum isteği gönderilemedi.");
            logger.warn("User {} tried to request room status for room {} but is not a member.", userEmail, roomCode);
        }
    }

    @MessageMapping("/chat/startSelection/{roomCode}")
    public void startSelection(@DestinationVariable String roomCode, @Payload @Valid StartSelectionRequestDTO request, Principal principal) {
        String userEmail = principal.getName();

        // Authority checks
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ADMIN"));
        if (!isAdmin) {
            messagingTemplate.convertAndSendToUser(userEmail, "/queue/chat-error", "Takım seçme sürecini başlatmak için yönetici yetkiniz bulunmamaktadır.");
            logger.warn("User {} tried to start selection in room {} without ADMIN role.", userEmail, roomCode);
            return;
        }

        if (!roomMembers.containsKey(roomCode) || !roomMembers.get(roomCode).contains(userEmail)) {
            messagingTemplate.convertAndSendToUser(userEmail, "/queue/chat-error", "Geçersiz oda veya odada değilsiniz.");
            logger.warn("User {} tried to start selection in room {} but is not a member.", userEmail, roomCode);
            return;
        }

        if (selectionInProgressMap.getOrDefault(roomCode, false)) {
            messagingTemplate.convertAndSendToUser(userEmail, "/queue/chat-error", "Takım seçme süreci zaten devam ediyor.");
            logger.warn("Selection already in progress for room {}", roomCode);
            return;
        }

        // Check if captains are set
        if (!teamACaptains.containsKey(roomCode) || !teamBCaptains.containsKey(roomCode)) {
            messagingTemplate.convertAndSendToUser(userEmail, "/queue/chat-error", "Takım seçimi başlatılamadı. Önce takım kaptanlarını belirlemelisiniz.");
            logger.warn("Cannot start selection in room {}: Captains not set.", roomCode);
            return;
        }

        List<PlayerDTO> playersToSelectFrom = request.getPlayersToSelectFrom();
        if (playersToSelectFrom == null || playersToSelectFrom.isEmpty()) {
            messagingTemplate.convertAndSendToUser(userEmail, "/queue/chat-error", "Lütfen seçilecek oyuncu listesini gönderin.");
            logger.warn("No players provided to start selection for room {}", roomCode);
            return;
        }

        // Initialize selection state for the room
        selectionInProgressMap.put(roomCode, true);
        availablePlayersForSelectionMap.put(roomCode, playersToSelectFrom);
        teamASelectedPlayersMap.put(roomCode, new ConcurrentHashMap<>());
        teamBSelectedPlayersMap.put(roomCode, new ConcurrentHashMap<>());
        currentSelectionRoundMap.put(roomCode, 0); // Start with round 0

        // Determine who picks first (e.g., Team A captain)
        currentPlayerSelectionTurnMap.put(roomCode, teamACaptains.get(roomCode));

        ChatMessageDTO startMessage = new ChatMessageDTO(
                "Sistem",
                String.format("Takım seçme süreci başladı! İlk seçim sırası: %s", currentPlayerSelectionTurnMap.get(roomCode).split("@")[0])
        );
        messagingTemplate.convertAndSend("/topic/chat/" + roomCode, startMessage);

        broadcastRoomStatus(roomCode);
        logger.info("Team selection started for room {}. First turn: {}", roomCode, currentPlayerSelectionTurnMap.get(roomCode));
    }

    @MessageMapping("/chat/selectPlayer/{roomCode}")
    public void selectPlayer(@DestinationVariable String roomCode, @Payload @Valid PlayerSelectDTO request, Principal principal) {
        String userEmail = principal.getName();

        if (!selectionInProgressMap.getOrDefault(roomCode, false)) {
            messagingTemplate.convertAndSendToUser(userEmail, "/queue/chat-error", "Takım seçme süreci şu anda aktif değil.");
            logger.warn("User {} tried to select player in room {} but selection is not in progress.", userEmail, roomCode);
            return;
        }

        // Check if it's the current user's turn
        if (!Objects.equals(currentPlayerSelectionTurnMap.get(roomCode), userEmail)) {
            messagingTemplate.convertAndSendToUser(userEmail, "/queue/chat-error", "Şu anda seçim sırası sizde değil.");
            logger.warn("User {} tried to select player but it's not their turn in room {}. Current turn: {}", userEmail, roomCode, currentPlayerSelectionTurnMap.get(roomCode));
            return;
        }

        // Check if the user is a captain
        String teamACaptain = teamACaptains.get(roomCode);
        String teamBCaptain = teamBCaptains.get(roomCode);
        if (!userEmail.equals(teamACaptain) && !userEmail.equals(teamBCaptain)) {
            messagingTemplate.convertAndSendToUser(userEmail, "/queue/chat-error", "Sadece kaptanlar oyuncu seçimi yapabilir.");
            logger.warn("User {} tried to select player in room {} but is not a captain.", userEmail, roomCode);
            return;
        }

        String selectedPlayerId = request.getPlayerId();
        List<PlayerDTO> currentAvailablePlayers = availablePlayersForSelectionMap.get(roomCode);

        PlayerDTO selectedPlayerDTO = currentAvailablePlayers.stream()
                .filter(p -> p.getId().toString().equals(selectedPlayerId))
                .findFirst()
                .orElse(null);

        if (selectedPlayerDTO == null) {
            messagingTemplate.convertAndSendToUser(userEmail, "/queue/chat-error", "Seçilen oyuncu listede bulunamadı veya daha önce seçildi.");
            logger.warn("Selected player {} not found in available list for room {}", selectedPlayerId, roomCode);
            return;
        }

        Map<String, PlayerDTO> teamA = teamASelectedPlayersMap.get(roomCode);
        Map<String, PlayerDTO> teamB = teamBSelectedPlayersMap.get(roomCode);

        // Determine which team the current captain belongs to
        String captainTeamIdentifier = null;
        if (userEmail.equals(teamACaptain)) {
            captainTeamIdentifier = "A";
        } else if (userEmail.equals(teamBCaptain)) {
            captainTeamIdentifier = "B";
        }

        Map<String, PlayerDTO> targetTeamPlayers = (captainTeamIdentifier.equals("A")) ? teamA : teamB;

        if (targetTeamPlayers.size() >= 7) { // Max 7 players per team
            messagingTemplate.convertAndSendToUser(userEmail, "/queue/chat-error", "Takımınız maksimum oyuncu sayısına ulaştı (7 oyuncu).");
            logger.warn("Team {} has reached max players (7) in room {}. Player {} not added.", captainTeamIdentifier, roomCode, selectedPlayerId);
            return;
        }

        // Add player to the team
        targetTeamPlayers.put(selectedPlayerId, selectedPlayerDTO);
        // Remove player from available pool
        availablePlayersForSelectionMap.put(roomCode,
                currentAvailablePlayers.stream()
                        .filter(p -> !p.getId().toString().equals(selectedPlayerId))
                        .collect(Collectors.toList())
        );

        ChatMessageDTO selectMessage = new ChatMessageDTO(
                "Sistem",
                String.format("%s, Takım %s için %s adlı oyuncuyu seçti.", userEmail.split("@")[0], captainTeamIdentifier, selectedPlayerDTO.getName())
        );
        messagingTemplate.convertAndSend("/topic/chat/" + roomCode, selectMessage);

        // Advance turn and round
        int currentRound = currentSelectionRoundMap.get(roomCode);
        String nextTurnEmail;

        if (teamA.size() + teamB.size() >= 14) { // 7 players per team, total 14
            
            broadcastRoomStatus(roomCode);
            nextTurnEmail = null;
            ChatMessageDTO endMessage = new ChatMessageDTO("Sistem", "Oyuncu seçimi tamamlandı! Takımlar kuruldu.");
            messagingTemplate.convertAndSend("/topic/chat/" + roomCode, endMessage);
            logger.info("Team selection finished for room {}.", roomCode);
            selectionInProgressMap.put(roomCode, false);
        } else {
            // A-B-A-B... pick order logic
            if (userEmail.equals(teamACaptain)) {
                nextTurnEmail = teamBCaptain;
            } else {
                nextTurnEmail = teamACaptain;
            }

            // Check if selection is balanced (A picks, then B picks, then A picks, etc.)
            // If one team is lagging behind, give turn to them
            if (teamA.size() > teamB.size() && userEmail.equals(teamACaptain)) {
                 nextTurnEmail = teamBCaptain; // A picked, B is behind, give B another turn
            } else if (teamB.size() > teamA.size() && userEmail.equals(teamBCaptain)) {
                 nextTurnEmail = teamACaptain; // B picked, A is behind, give A another turn
            }
            // If sizes are equal, alternate based on last picker
            // The default logic `if (userEmail.equals(teamACaptain)) { nextTurnEmail = teamBCaptain; }` handles equal sizes alternation implicitly.

            currentSelectionRoundMap.put(roomCode, currentRound + 1);
            ChatMessageDTO nextTurnMessage = new ChatMessageDTO(
                    "Sistem",
                    String.format("Sıradaki seçim sırası: %s", nextTurnEmail.split("@")[0])
            );
            messagingTemplate.convertAndSend("/topic/chat/" + roomCode, nextTurnMessage);
            logger.info("Next turn in room {}: {}", roomCode, nextTurnEmail);
        }
        currentPlayerSelectionTurnMap.put(roomCode, nextTurnEmail);

        broadcastRoomStatus(roomCode);
    }


    public void handleUserDisconnect(String userEmail) {
        String roomCode = null;
        for (Map.Entry<String, Set<String>> entry : roomMembers.entrySet()) {
            if (entry.getValue().remove(userEmail)) {
                roomCode = entry.getKey();
                logger.info("User {} removed from room {}", userEmail, roomCode);
                break;
            }
        }

        if (roomCode != null) {
            if (roomMembers.get(roomCode).isEmpty()) {
                roomMembers.remove(roomCode);
                roomCodeOwnerMap.remove(roomCode);
                teamACaptains.remove(roomCode);
                teamBCaptains.remove(roomCode);
                selectionInProgressMap.remove(roomCode);
                availablePlayersForSelectionMap.remove(roomCode);
                teamASelectedPlayersMap.remove(roomCode);
                teamBSelectedPlayersMap.remove(roomCode);
                currentPlayerSelectionTurnMap.remove(roomCode);
                currentSelectionRoundMap.remove(roomCode);
                logger.info("Room {} is empty and removed. All related selection states cleared.", roomCode);
            } else {
                // If a captain disconnects, clear their role. Selection process might need to be reset.
                if (userEmail.equals(teamACaptains.get(roomCode))) {
                    teamACaptains.remove(roomCode);
                    selectionInProgressMap.put(roomCode, false); // Stop selection if captain leaves
                    ChatMessageDTO captainLeftMessage = new ChatMessageDTO("Sistem", "Takım A Kaptanı bağlantısı kesildi. Takım seçimi durduruldu.");
                    messagingTemplate.convertAndSend("/topic/chat/" + roomCode, captainLeftMessage);
                    logger.info("Team A Captain {} disconnected, clearing role and stopping selection for room {}.", userEmail, roomCode);
                }
                if (userEmail.equals(teamBCaptains.get(roomCode))) {
                    teamBCaptains.remove(roomCode);
                    selectionInProgressMap.put(roomCode, false); // Stop selection if captain leaves
                    ChatMessageDTO captainLeftMessage = new ChatMessageDTO("Sistem", "Takım B Kaptanı bağlantısı kesildi. Takım seçimi durduruldu.");
                    messagingTemplate.convertAndSend("/topic/chat/" + roomCode, captainLeftMessage);
                    logger.info("Team B Captain {} disconnected, clearing role and stopping selection for room {}.", userEmail, roomCode);
                }

                ChatMessageDTO leaveMessage = new ChatMessageDTO("Sistem", userEmail.split("@")[0] + " adlı kullanıcı odadan ayrıldı.");
                messagingTemplate.convertAndSend("/topic/chat/" + roomCode, leaveMessage);
                broadcastRoomStatus(roomCode); // Broadcast updated room status (without disconnected user)
            }
        }
    }

    private void broadcastRoomStatus(String roomCode) {
        Set<String> currentMembers = roomMembers.getOrDefault(roomCode, Collections.emptySet());
        RoomStatusDTO roomStatus = RoomStatusDTO.builder()
                .roomCode(roomCode)
                .usersInRoom(currentMembers.stream().sorted().collect(Collectors.toList()))
                .teamACaptainEmail(teamACaptains.get(roomCode))
                .teamBCaptainEmail(teamBCaptains.get(roomCode))
                .selectionInProgress(selectionInProgressMap.getOrDefault(roomCode, false))
                .availablePlayersForSelection(availablePlayersForSelectionMap.getOrDefault(roomCode, Collections.emptyList()))
                .teamASelectedPlayers(teamASelectedPlayersMap.getOrDefault(roomCode, Collections.emptyMap()))
                .teamBSelectedPlayers(teamBSelectedPlayersMap.getOrDefault(roomCode, Collections.emptyMap()))
                .currentPlayerSelectionTurnEmail(currentPlayerSelectionTurnMap.get(roomCode))
                .selectionStatusMessage(getSelectionStatusMessage(roomCode))
                .build();
        messagingTemplate.convertAndSend("/topic/room-status/" + roomCode, roomStatus);
        logger.debug("Broadcasted room status for room {}: {}", roomCode, roomStatus);
    }

    private void sendRoomStatusToUser(String roomCode, String userEmail) {
        Set<String> currentMembers = roomMembers.getOrDefault(roomCode, Collections.emptySet());
        RoomStatusDTO roomStatus = RoomStatusDTO.builder()
                .roomCode(roomCode)
                .usersInRoom(currentMembers.stream().sorted().collect(Collectors.toList()))
                .teamACaptainEmail(teamACaptains.get(roomCode))
                .teamBCaptainEmail(teamBCaptains.get(roomCode))
                .selectionInProgress(selectionInProgressMap.getOrDefault(roomCode, false))
                .availablePlayersForSelection(availablePlayersForSelectionMap.getOrDefault(roomCode, Collections.emptyList()))
                .teamASelectedPlayers(teamASelectedPlayersMap.getOrDefault(roomCode, Collections.emptyMap()))
                .teamBSelectedPlayers(teamBSelectedPlayersMap.getOrDefault(roomCode, Collections.emptyMap()))
                .currentPlayerSelectionTurnEmail(currentPlayerSelectionTurnMap.get(roomCode))
                .selectionStatusMessage(getSelectionStatusMessage(roomCode))
                .build();
        messagingTemplate.convertAndSendToUser(userEmail, "/queue/room-status/" + roomCode, roomStatus);
        logger.debug("Sent specific room status to user {}: for room {}: {}", userEmail, roomCode, roomStatus);
    }

    private String getSelectionStatusMessage(String roomCode) {
        if (!selectionInProgressMap.getOrDefault(roomCode, false)) {
            return "Takım seçme süreci aktif değil.";
        }

        Map<String, PlayerDTO> teamA = teamASelectedPlayersMap.get(roomCode);
        Map<String, PlayerDTO> teamB = teamBSelectedPlayersMap.get(roomCode);
        String currentTurnEmail = currentPlayerSelectionTurnMap.get(roomCode);

        String message = String.format("Takım A: %d oyuncu, Takım B: %d oyuncu. Kalan oyuncu: %d.",
                teamA.size(), teamB.size(), availablePlayersForSelectionMap.getOrDefault(roomCode, Collections.emptyList()).size());

        if (currentTurnEmail != null) {
            message += String.format(" Sıradaki seçim: %s", currentTurnEmail.split("@")[0]);
        }
        return message;
    }

    private String generateUniqueRoomCode() {
        String roomCode;
        do {
            // Generate a 6-digit number
            int randomNum = ThreadLocalRandom.current().nextInt(100000, 1000000);
            roomCode = String.valueOf(randomNum);
        } while (roomMembers.containsKey(roomCode));
        return roomCode;
    }
}