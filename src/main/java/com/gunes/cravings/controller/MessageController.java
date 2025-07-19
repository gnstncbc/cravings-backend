package com.gunes.cravings.controller;

import java.security.Principal;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;

import com.gunes.cravings.dto.ChatMessageDTO;
import com.gunes.cravings.dto.RoomStatusDTO;
import com.gunes.cravings.dto.SetCaptainsRequestDTO;

import jakarta.validation.Valid;

@Controller
public class MessageController {

    private static final Logger logger = LoggerFactory.getLogger(MessageController.class);

    private final SimpMessagingTemplate messagingTemplate;

    private final Map<String, Set<String>> roomMembers = new ConcurrentHashMap<>();
    private final Map<String, String> roomCodeOwnerMap = new ConcurrentHashMap<>();
    private final Map<String, String> teamACaptains = new ConcurrentHashMap<>();
    private final Map<String, String> teamBCaptains = new ConcurrentHashMap<>();


    public MessageController(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
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

        String roomCode = generateUniqueRoomCode();
        roomMembers.put(roomCode, Collections.synchronizedSet(new HashSet<>()));
        roomCodeOwnerMap.put(roomCode, userEmail);

        roomMembers.get(roomCode).add(userEmail);
        logger.info("User {} added to room {}", userEmail, roomCode);

        messagingTemplate.convertAndSendToUser(userEmail, "/queue/room-created", roomCode);
        messagingTemplate.convertAndSendToUser(userEmail, "/queue/room-joined", roomCode);

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
            messagingTemplate.convertAndSendToUser(userEmail, "/queue/room-join-error", "Zaten bir odadasınız: " + existingRoom);
            logger.warn("User {} tried to join room {} but is already in room {}", userEmail, roomCode, existingRoom);
            return;
        }

        if (!roomMembers.containsKey(roomCode)) {
            messagingTemplate.convertAndSendToUser(userEmail, "/queue/room-join-error", "Oda kodu geçersiz veya mevcut değil: " + roomCode);
            logger.warn("Join error: Room code is not active: {} Requested by: {}", roomCode, userEmail);
            return;
        }

        roomMembers.get(roomCode).add(userEmail);
        logger.info("User {} added to room {}", userEmail, roomCode);

        messagingTemplate.convertAndSendToUser(userEmail, "/queue/room-joined", roomCode);

        ChatMessageDTO joinMessage = new ChatMessageDTO("Sistem", userEmail.split("@")[0] + " adlı kullanıcı odaya katıldı.");
        messagingTemplate.convertAndSend("/topic/chat/" + roomCode, joinMessage);

        logger.info("User {} joined room: {}", userEmail, roomCode);
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
        logger.info("Message sent to room {}: From {} - {}", roomCode, senderEmail, chatMessage.getContent());
        return chatMessage;
    }

    @MessageMapping("/chat/setCaptains/{roomCode}")
    public void setCaptains(@DestinationVariable String roomCode, @Payload SetCaptainsRequestDTO request, Principal principal) {
        // YENİ KONTROL: Authentication objesinin null olup olmadığını kontrol et
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
            "Kaptanlar belirlendi! \n" + " Takım A Kaptanı: " + teamACaptainName + "\n Takım B Kaptanı: " + teamBCaptainName
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
                logger.info("Room {} is empty and removed.", roomCode);
            } else {
                if (userEmail.equals(teamACaptains.get(roomCode))) {
                    teamACaptains.remove(roomCode);
                    logger.info("Team A Captain {} disconnected, clearing role for room {}.", userEmail, roomCode);
                }
                if (userEmail.equals(teamBCaptains.get(roomCode))) {
                    teamBCaptains.remove(roomCode);
                    logger.info("Team B Captain {} disconnected, clearing role for room {}.", userEmail, roomCode);
                }
                ChatMessageDTO leaveMessage = new ChatMessageDTO("Sistem", userEmail.split("@")[0] + " adlı kullanıcı odadan ayrıldı.");
                messagingTemplate.convertAndSend("/topic/chat/" + roomCode, leaveMessage);
                broadcastRoomStatus(roomCode);
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
                .build();
        messagingTemplate.convertAndSendToUser(userEmail, "/queue/room-status/" + roomCode, roomStatus);
        logger.debug("Sent specific room status to user {}: for room {}: {}", userEmail, roomCode, roomStatus);
    }

    private String generateUniqueRoomCode() {
        String roomCode;
        do {
            int randomNum = ThreadLocalRandom.current().nextInt(100000, 1000000);
            roomCode = String.valueOf(randomNum);
        } while (roomMembers.containsKey(roomCode));
        return roomCode;
    }
}