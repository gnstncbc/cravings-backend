package com.gunes.cravings.config;

import com.gunes.cravings.config.JwtService; 
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureException;
import io.jsonwebtoken.UnsupportedJwtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
// Authentication import'u buraya da gerekebilir, eğer kullanılıyorsa
import org.springframework.security.core.Authentication; 
import org.springframework.security.core.context.SecurityContextHolder; // Bu import kalacak
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;

@Component
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketAuthInterceptor.class);

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    public WebSocketAuthInterceptor(JwtService jwtService, @Lazy UserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            List<String> authorizationHeaders = accessor.getNativeHeader("Authorization");
            String jwt = null;

            if (authorizationHeaders != null && !authorizationHeaders.isEmpty()) {
                String bearerToken = authorizationHeaders.get(0);
                if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
                    jwt = bearerToken.substring(7);
                }
            }

            if (jwt != null) {
                try {
                    String userEmail = jwtService.extractUsername(jwt);
                    if (userEmail != null) {
                        UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);
                        if (jwtService.isTokenValid(jwt, userDetails)) {
                            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities()
                            );
                            accessor.setUser(authentication);
                            // DEĞİŞİKLİK: SecurityContextHolder.getContext().setAuthentication(authentication); satırı buradan kaldırıldı.
                            logger.info("WebSocket CONNECT: User {} authenticated successfully and Principal set on accessor.", userEmail);
                        } else {
                            logger.warn("WebSocket CONNECT: JWT token is invalid for user: {}", userEmail);
                            // SecurityContextHolder.clearContext(); // Buradan kaldırıldı
                        }
                    } else {
                        logger.warn("WebSocket CONNECT: User email could not be extracted from JWT.");
                        // SecurityContextHolder.clearContext(); // Buradan kaldırıldı
                    }
                } catch (ExpiredJwtException ex) {
                    logger.warn("WebSocket CONNECT: Expired JWT token for {}: {}", accessor.getFirstNativeHeader("login"), ex.getMessage());
                    // SecurityContextHolder.clearContext(); // Buradan kaldırıldı
                } catch (UnsupportedJwtException | MalformedJwtException | SignatureException | IllegalArgumentException ex) {
                    logger.warn("WebSocket CONNECT: Invalid JWT token format or signature: {}", ex.getMessage());
                    // SecurityContextHolder.clearContext(); // Buradan kaldırıldı
                } catch (UsernameNotFoundException ex) {
                    logger.warn("WebSocket CONNECT: User not found for email extracted from JWT: {}", ex.getMessage());
                    // SecurityContextHolder.clearContext(); // Buradan kaldırıldı
                } catch (Exception e) {
                    logger.error("WebSocket CONNECT: An unexpected error occurred during JWT processing: {}", e.getMessage(), e);
                    // SecurityContextHolder.clearContext(); // Buradan kaldırıldı
                }
            } else {
                logger.debug("WebSocket CONNECT: No JWT token found in Authorization header.");
                // SecurityContextHolder.clearContext(); // Buradan kaldırıldı
            }
        }
        // DEĞİŞİKLİK: CONNECT dışındaki komutlar için SecurityContextHolder ayarlama mantığı kaldırıldı,
        // çünkü bu işi SecurityContextChannelInterceptor yapacak.
        // if (accessor.getUser() instanceof Authentication) {
        //     SecurityContextHolder.getContext().setAuthentication((Authentication) accessor.getUser());
        // } else {
        //     SecurityContextHolder.clearContext();
        // }

        return message;
    }

    @Override
    public void afterSendCompletion(Message<?> message, MessageChannel channel, boolean sent, Exception ex) {
        // DEĞİŞİKLİK: SecurityContextHolder.clearContext(); satırı buradan kaldırıldı.
        // Bu temizleme işini SecurityContextChannelInterceptor üstlenecek.
        // logger.debug("SecurityContextHolder cleared after message processing.");
    }
}