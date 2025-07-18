package com.gunes.cravings.controller;

import com.gunes.cravings.dto.ChatMessageDTO;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import jakarta.validation.Valid;

@Controller
public class MessageController {

    private final SimpMessagingTemplate messagingTemplate;
    // Oda kodlarını ve sahiplerini tutmak için basit bir yapı.
    // Gerçek bir uygulamada bu veritabanında veya dağıtılmış bir önbellekte tutulmalıdır.
    private final Set<String> activeRoomCodes = Collections.synchronizedSet(new HashSet<>());
    private final Map<String, String> roomCodeOwnerMap = new ConcurrentHashMap<>();

    public MessageController(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/chat/createRoom")
    public void createRoom(Principal principal) {
        String roomCode = generateUniqueRoomCode();
        activeRoomCodes.add(roomCode);
        roomCodeOwnerMap.put(roomCode, principal.getName());
        // Odayı oluşturan kullanıcıya özel bir queue üzerinden oda kodunu gönder
        messagingTemplate.convertAndSendToUser(principal.getName(), "/queue/room-created", roomCode);
        // Odaya katıldığını kendi kendine bildir (isteğe bağlı, frontend'de kontrol edilebilir)
        messagingTemplate.convertAndSendToUser(principal.getName(), "/queue/room-joined", roomCode);
        // Genel odaya katılım mesajı (opsiyonel)
        ChatMessageDTO joinMessage = new ChatMessageDTO("Sistem", principal.getName() + " adlı kullanıcı odaya katıldı.");
        messagingTemplate.convertAndSend("/topic/chat/" + roomCode, joinMessage);
    }

    @MessageMapping("/chat/joinRoom/{roomCode}")
    public void joinRoom(@DestinationVariable String roomCode, Principal principal) {
        if (!activeRoomCodes.contains(roomCode)) {
            // Oda mevcut değilse kullanıcıya hata bildir
            messagingTemplate.convertAndSendToUser(principal.getName(), "/queue/room-join-error", "Oda kodu geçersiz veya mevcut değil: " + roomCode);
            System.err.println("Katılım hatası: Oda kodu aktif değil: " + roomCode + " İsteyen: " + principal.getName());
            return;
        }

        // Kullanıcıyı odaya başarıyla katıldığına dair bilgilendir
        messagingTemplate.convertAndSendToUser(principal.getName(), "/queue/room-joined", roomCode);
        System.out.println(principal.getName() + " kullanıcısı " + roomCode + " odasına katıldı.");

        // Odaya katılan diğer tüm kullanıcılara yeni bir kullanıcının katıldığını bildir
        ChatMessageDTO joinMessage = new ChatMessageDTO("Sistem", principal.getName() + " adlı kullanıcı odaya katıldı.");
        messagingTemplate.convertAndSend("/topic/chat/" + roomCode, joinMessage);
    }


    @MessageMapping("/chat/sendMessage/{roomCode}")
    public ChatMessageDTO sendMessage(@DestinationVariable String roomCode, @Payload @Valid ChatMessageDTO chatMessage, Principal principal) {
        if (!activeRoomCodes.contains(roomCode)) {
            System.err.println("Geçersiz veya aktif olmayan oda kodu: " + roomCode);
            return null;
        }

        chatMessage.setSender(principal.getName());
        messagingTemplate.convertAndSend("/topic/chat/" + roomCode, chatMessage);
        return chatMessage;
    }

    private String generateUniqueRoomCode() {
        String roomCode;
        do {
            int randomNum = ThreadLocalRandom.current().nextInt(100000, 1000000);
            roomCode = String.valueOf(randomNum);
        } while (activeRoomCodes.contains(roomCode));
        return roomCode;
    }
}