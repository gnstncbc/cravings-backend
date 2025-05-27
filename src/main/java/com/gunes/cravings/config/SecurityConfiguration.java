package com.gunes.cravings.config;

import com.gunes.cravings.model.Role;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@EnableMethodSecurity // Enables @PreAuthorize, @PostAuthorize, etc.
public class SecurityConfiguration {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final AuthenticationProvider authenticationProvider;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(req -> req
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/players/**").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/api/**").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/api/matches/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/matches/**").permitAll()
                        
                        // Admin specific endpoints
                        .requestMatchers("/api/admin/**").hasAuthority(Role.ADMIN.name()) // General admin path

                        // Match specific rules (redundant if /api/admin/** is stricter, but kept for clarity)
                        .requestMatchers(HttpMethod.POST, "/api/matches/**").hasAuthority(Role.ADMIN.name())
                        .requestMatchers(HttpMethod.PUT, "/api/matches/**").hasAuthority(Role.ADMIN.name())
                        .requestMatchers(HttpMethod.DELETE, "/api/matches/**").hasAuthority(Role.ADMIN.name())
                        .requestMatchers(HttpMethod.POST, "/api/matches/{matchId}/score").hasAuthority(Role.ADMIN.name())

                        // Player specific rules (redundant if /api/admin/** is stricter)
                        .requestMatchers(HttpMethod.DELETE, "/api/players/**").hasAuthority(Role.ADMIN.name())
                        .requestMatchers(HttpMethod.POST, "/api/players/**").hasAuthority(Role.ADMIN.name())
                        .requestMatchers(HttpMethod.PUT, "/api/players/**").hasAuthority(Role.ADMIN.name())
                        
                        // Poll related rules
                        .requestMatchers(HttpMethod.POST, "/api/polls/{matchId}/vote").hasAnyAuthority(Role.USER.name(), Role.ADMIN.name())
                        .requestMatchers(HttpMethod.GET, "/api/polls/{matchId}/results").hasAnyAuthority(Role.USER.name(), Role.ADMIN.name())
                        .requestMatchers(HttpMethod.GET, "/api/polls/{matchId}/user-votes").hasAuthority(Role.ADMIN.name())
                        
                        .anyRequest().authenticated())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(authenticationProvider)
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration
                .setAllowedOrigins(List.of("http://localhost:3000", "https://gnstncbc.com", "http://localhost:8080"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Cache-Control", "Content-Type",
                "X-Requested-With", "Accept", "Origin"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}