package com.gunes.cravings.config;

import com.gunes.cravings.model.User; // Eğer User modeliniz varsa
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.SignatureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.util.StringUtils;

import java.util.List;

public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketAuthInterceptor.class);

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    // JwtService ve UserDetailsService'i constructor injection ile alıyoruz
    public WebSocketAuthInterceptor(JwtService jwtService, UserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        // Sadece CONNECT komutunda JWT doğrulamasını yapıyoruz
        // Çünkü token bu aşamada client tarafından gönderilir.
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

                    if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                        UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);

                        if (jwtService.isTokenValid(jwt, userDetails)) {
                            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null, // Kimlik bilgisi zaten JWT ile doğrulanmıştır, şifreye gerek yok
                                    userDetails.getAuthorities()
                            );
                            // WebSocket bağlantısı için Spring Security bağlamına kimlik doğrulamayı ekle
                            accessor.setUser(authentication); 
                            // NOT: SecurityContextHolder.getContext().setAuthentication(authentication);
                            // buraya eklenmez, çünkü ChannelInterceptor'da her gelen WebSocket mesajı için yeni bir güvenlik bağlamı oluşturulur.
                            // accesor.setUser() WebSocket oturumu için yeterlidir.
                        } else {
                            logger.warn("WebSocket CONNECT: JWT token is invalid for user: {}", userEmail);
                            // Token geçersizse bağlantıyı reddedebiliriz, veya sadece authenticate etmeyiz.
                            // Örneğin throw new MessagingException("Invalid JWT Token");
                        }
                    }
                } catch (ExpiredJwtException ex) {
                    logger.warn("WebSocket CONNECT: Expired JWT token for {}: {}", accessor.getFirstNativeHeader("login"), ex.getMessage());
                    // Frontend'e hata mesajı göndermek için farklı bir strateji gerekebilir (örn. StompErrorFrame).
                } catch (UnsupportedJwtException | MalformedJwtException | SignatureException | IllegalArgumentException ex) {
                    logger.warn("WebSocket CONNECT: Invalid JWT token format or signature: {}", ex.getMessage());
                } catch (JwtException ex) {
                    logger.error("WebSocket CONNECT: General JWT error: {}", ex.getMessage(), ex);
                } catch (Exception e) {
                    logger.error("WebSocket CONNECT: An unexpected error occurred during JWT processing: {}", e.getMessage(), e);
                }
            } else {
                logger.debug("WebSocket CONNECT: No JWT token found in Authorization header.");
            }
        }
        return message; // Mesajı işlemeye devam et
    }
}