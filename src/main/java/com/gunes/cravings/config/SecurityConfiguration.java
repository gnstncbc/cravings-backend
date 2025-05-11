package com.gunes.cravings.config;

import com.gunes.cravings.model.Role;
import lombok.RequiredArgsConstructor;
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
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(req ->
                        req.requestMatchers("/api/auth/**").permitAll()
                                .requestMatchers(HttpMethod.GET, "/api/players/**").permitAll()
                                .requestMatchers(HttpMethod.OPTIONS, "/api/**").permitAll()
                                .requestMatchers(HttpMethod.OPTIONS, "/api/matches/**").permitAll()
                                .requestMatchers(HttpMethod.GET, "/api/matches/**").permitAll()
                                .requestMatchers(HttpMethod.POST, "/api/matches/**").hasAuthority(Role.ADMIN.name())
                                .requestMatchers(HttpMethod.PUT, "/api/matches/**").hasAuthority(Role.ADMIN.name()) // Assuming PUT might be used for updates
                                .requestMatchers(HttpMethod.DELETE, "/api/matches/**").hasAuthority(Role.ADMIN.name())
                                .requestMatchers(HttpMethod.DELETE, "/api/players/**").hasAuthority(Role.ADMIN.name())
                                .requestMatchers(HttpMethod.POST, "/api/players/**").hasAuthority(Role.ADMIN.name())
                                .requestMatchers(HttpMethod.PUT, "/api/players/**").hasAuthority(Role.ADMIN.name())
                                .requestMatchers(HttpMethod.GET, "/api/players/**").permitAll()
                                .requestMatchers(HttpMethod.GET, "/api/matches/**").hasAnyAuthority(Role.USER.name(), Role.ADMIN.name())
                                .requestMatchers(HttpMethod.POST, "/api/matches").hasAuthority(Role.ADMIN.name()) // Create match
                                .requestMatchers(HttpMethod.POST, "/api/matches/{matchId}/score").hasAuthority(Role.ADMIN.name()) // Score endpoint
                                .requestMatchers(HttpMethod.PUT, "/api/matches/**").hasAuthority(Role.ADMIN.name())
                                .requestMatchers(HttpMethod.DELETE, "/api/matches/**").hasAuthority(Role.ADMIN.name())
                                .requestMatchers(HttpMethod.POST, "/api/polls/{matchId}/vote").hasAnyAuthority(Role.USER.name(), Role.ADMIN.name())
                                .requestMatchers(HttpMethod.GET, "/api/polls/{matchId}/results").hasAnyAuthority(Role.USER.name(), Role.ADMIN.name())
                                .requestMatchers(HttpMethod.GET, "/api/polls/{matchId}/user-votes").hasAuthority(Role.ADMIN.name())
                                .anyRequest().authenticated()
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(authenticationProvider)
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
} 