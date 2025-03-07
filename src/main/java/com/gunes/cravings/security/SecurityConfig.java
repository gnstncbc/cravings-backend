package com.gunes.cravings.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(withDefaults())
                .csrf(csrf -> csrf.disable()) // CSRF'yi devre dışı bırak (POST request’lerde sorun olmaması için)
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll()); // Herkese izin ver (test amaçlı)
        return http.build();
    }
}
