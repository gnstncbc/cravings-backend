package com.gunes.cravings.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.ExpiredJwtException; // Bunu import edin
import io.jsonwebtoken.JwtException;       // Genel JWT exception'ları için
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.SignatureException; // İmza hataları için
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        String userEmail = null; // Initialize userEmail

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        jwt = authHeader.substring(7);

        try {
            userEmail = jwtService.extractUsername(jwt);

            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);
                if (jwtService.isTokenValid(jwt, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );
                    authToken.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request)
                    );
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                } else {
                    // Token geçerli değilse (örneğin, süre dolmuş olabilir ama extractUsername başarılı olduysa)
                    // Bu durum genellikle isTokenExpired tarafından yakalanır.
                    // CustomAuthenticationEntryPoint bu durumu yönetecektir.
                    logger.debug("JWT token is invalid for user: {}", userEmail);
                }
            }
            filterChain.doFilter(request, response);

        } catch (ExpiredJwtException ex) {
            logger.warn("JWT token has expired for URI {}: {}", request.getRequestURI(), ex.getMessage());
            sendErrorResponse(response, HttpStatus.UNAUTHORIZED, "Oturum süresi doldu. Lütfen tekrar giriş yapın.", request.getRequestURI());
        } catch (UnsupportedJwtException ex) {
            logger.warn("Unsupported JWT token for URI {}: {}", request.getRequestURI(), ex.getMessage());
            sendErrorResponse(response, HttpStatus.UNAUTHORIZED, "Desteklenmeyen JWT formatı.", request.getRequestURI());
        } catch (MalformedJwtException ex) {
            logger.warn("Malformed JWT token for URI {}: {}", request.getRequestURI(), ex.getMessage());
            sendErrorResponse(response, HttpStatus.UNAUTHORIZED, "Geçersiz JWT formatı.", request.getRequestURI());
        } catch (SignatureException ex) {
            logger.warn("JWT signature validation failed for URI {}: {}", request.getRequestURI(), ex.getMessage());
            sendErrorResponse(response, HttpStatus.UNAUTHORIZED, "JWT imzası geçersiz.", request.getRequestURI());
        } catch (IllegalArgumentException ex) {
            logger.warn("JWT claims string is empty or invalid for URI {}: {}", request.getRequestURI(), ex.getMessage());
            sendErrorResponse(response, HttpStatus.UNAUTHORIZED, "JWT içeriği geçersiz.", request.getRequestURI());
        } catch (UsernameNotFoundException ex) {
            // Bu, token'dan kullanıcı adı çıkarıldıktan sonra UserDetailsService'in kullanıcıyı bulamaması durumudur.
            // Bu, token geçerli olsa bile kullanıcının artık sistemde olmadığı anlamına gelebilir.
            logger.warn("User not found for email extracted from JWT ({}): {}", userEmail, ex.getMessage());
            sendErrorResponse(response, HttpStatus.UNAUTHORIZED, "Token'a ait kullanıcı bulunamadı.", request.getRequestURI());
        } catch (JwtException ex) { // Diğer beklenmedik JWT hataları için genel bir catch
            logger.error("An unexpected JWT error occurred for URI {}: {}", request.getRequestURI(), ex.getMessage(), ex);
            sendErrorResponse(response, HttpStatus.UNAUTHORIZED, "JWT ile ilgili beklenmedik bir hata oluştu.", request.getRequestURI());
        }
        // filterChain.doFilter(request, response) yukarıda try bloğunun sonunda çağrıldığı için
        // exception durumlarında tekrar çağrılmaz, çünkü yanıt zaten gönderilmiştir.
    }

    private void sendErrorResponse(HttpServletResponse response, HttpStatus status, String message, String path) throws IOException {
        response.setStatus(status.value());
        response.setContentType("application/json;charset=UTF-8");

        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", System.currentTimeMillis());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        body.put("path", path);

        objectMapper.writeValue(response.getWriter(), body);
    }
}