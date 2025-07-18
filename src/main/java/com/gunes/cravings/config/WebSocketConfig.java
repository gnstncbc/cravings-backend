package com.gunes.cravings.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.messaging.simp.config.ChannelRegistration; // Bu import'u ekleyin
import org.springframework.security.core.userdetails.UserDetailsService; // Bu import'u ekleyin

@Configuration
@EnableWebSocketMessageBroker // WebSocket mesaj işleme özelliğini etkinleştirir
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    // JwtService ve UserDetailsService'i buraya inject ediyoruz
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    // Constructor Injection ile bağımlılıkları alıyoruz
    public WebSocketConfig(JwtService jwtService, UserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.setApplicationDestinationPrefixes("/app");
        config.enableSimpleBroker("/topic", "/queue"); // /queue prefix'ini ekledik
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOrigins("http://localhost:3000") // CORS ayarınız
                .withSockJS();
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // Gelen mesajlar için ChannelInterceptor'ı kaydedin
        registration.interceptors(new WebSocketAuthInterceptor(jwtService, userDetailsService));
    }
}