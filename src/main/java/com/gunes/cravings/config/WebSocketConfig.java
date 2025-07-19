package com.gunes.cravings.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.authentication.AuthenticationManager; // Yeni import
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationProvider; // Yeni import
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder; // Yeni import
import org.springframework.security.authentication.ProviderManager; // Yeni import
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken; // Yeni import
import org.springframework.security.core.context.SecurityContextHolder; // Yeni import
import org.springframework.security.core.Authentication; // Yeni import
import org.springframework.security.web.context.SecurityContextHolderFilter; // Belki bu da gerekir

// SecurityContextChannelInterceptor için importlar
import org.springframework.security.messaging.context.AuthenticationPrincipalArgumentResolver;
import org.springframework.security.messaging.context.SecurityContextChannelInterceptor;


@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final WebSocketAuthInterceptor webSocketAuthInterceptor; 
    // YENİ EKLENEN: AuthenticationManager'ı inject ediyoruz.
    // Bu, SecurityContextChannelInterceptor'ın ihtiyaç duyabileceği bir bileşen olabilir.
    private final AuthenticationManager authenticationManager;


    public WebSocketConfig(JwtService jwtService, UserDetailsService userDetailsService, WebSocketAuthInterceptor webSocketAuthInterceptor, AuthenticationManager authenticationManager) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.webSocketAuthInterceptor = webSocketAuthInterceptor;
        this.authenticationManager = authenticationManager; // Inject edilen AuthenticationManager
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.setApplicationDestinationPrefixes("/app");
        config.enableSimpleBroker("/topic", "/queue");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOrigins("http://localhost:3000", "https://gnstncbc.com", "http://localhost:8080")
                .withSockJS();
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // ÇOK ÖNEMLİ SIRALAMA:
        // 1. WebSocketAuthInterceptor: JWT'yi doğrular ve Principal'ı StompHeaderAccessor'a ayarlar.
        // 2. SecurityContextChannelInterceptor: StompHeaderAccessor'daki Principal'ı alıp
        //    SecurityContextHolder'a bağlar ve işlem sonunda temizler.

        // NOT: Spring Security'nin kendi SecurityContextChannelInterceptor'ı,
        // Authentication objesini kendisi oluşturmaz. Sadece var olanı yayar.
        // Bu yüzden önce bizim interceptor'ımızın Authentication objesini Principal olarak ayarlaması lazım.

        registration.interceptors(
            webSocketAuthInterceptor, // Önce bizim interceptor'ımız JWT'yi işlesin
            new SecurityContextChannelInterceptor() // Sonra bu interceptor SecurityContextHolder'ı yönetsin
        );
    }

    /*
     * Not: AuthenticationManager'ın nasıl inject edileceği SecurityConfiguration'dan gelir.
     * Eğer zaten ApplicationConfig içinde bir AuthenticationManager bean'iniz varsa,
     * onu burada inject etmek yeterli olacaktır.
     * SecurityConfiguration'da bir @Bean olarak sunulmuş olmalı:
     * @Bean
     * public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
     * return config.getAuthenticationManager();
     * }
     * Bu durumda yukarıdaki constructor Injection otomatik olarak çalışır.
     */
}