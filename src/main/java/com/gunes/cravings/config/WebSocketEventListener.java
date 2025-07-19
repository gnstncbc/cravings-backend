package com.gunes.cravings.config;

import com.gunes.cravings.controller.MessageController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import java.security.Principal;

@Component
public class WebSocketEventListener implements ApplicationListener<SessionDisconnectEvent> {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketEventListener.class);

    private final MessageController messageController; // MessageController enjekte ediliyor
    private final SimpMessagingTemplate messagingTemplate; // Gerekli olabilir, ancak şimdilik direkt MessageController üzerinden iletişim kuruyoruz.

    public WebSocketEventListener(MessageController messageController, SimpMessagingTemplate messagingTemplate) {
        this.messageController = messageController;
        this.messagingTemplate = messagingTemplate;
    }

    @Override
    public void onApplicationEvent(SessionDisconnectEvent event) {
        Principal principal = event.getUser();
        if (principal != null) {
            String userEmail = principal.getName();
            logger.info("User disconnected: {}", userEmail);
            // Kullanıcı kaldırma ve oda durumu güncelleme işlemini MessageController'a devrediyoruz
            messageController.handleUserDisconnect(userEmail);
        } else {
            logger.warn("Disconnected session without a principal.");
        }
    }
}