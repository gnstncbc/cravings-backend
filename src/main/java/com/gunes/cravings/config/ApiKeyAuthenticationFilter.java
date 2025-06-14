package com.gunes.cravings.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
@RequiredArgsConstructor
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    @Value("${api.key.header:X-API-KEY}") // API anahtarının bulunacağı header adı
    private String apiKeyHeader;

    @Value("${api.key.value}") // Doğru API anahtarı değeri
    private String secretApiKey;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        // Filtreyi sadece belirtilen path için çalıştır
        if (!request.getRequestURI().startsWith("/api/para/")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Header'dan API anahtarını al
        final String requestApiKey = request.getHeader(apiKeyHeader);

        // Anahtar yoksa veya yanlışsa isteği reddet
        if (requestApiKey == null || !requestApiKey.equals(secretApiKey)) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Geçersiz API Anahtarı");
            return;
        }

        // Anahtar doğruysa, kimlik doğrulaması yap ve yetki ata
        // Bu sayede @PreAuthorize veya security config'de bu rolü kontrol edebiliriz
        var authentication = new UsernamePasswordAuthenticationToken(
                "api-user", // Principal olarak kullanılacak bir isim
                null,
                Collections.singleton(new SimpleGrantedAuthority("ROLE_API")) // Bu isteğe özel bir yetki
        );

        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        filterChain.doFilter(request, response);
    }
}