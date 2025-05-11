package com.gunes.cravings.service;

import com.gunes.cravings.config.JwtService;
import com.gunes.cravings.dto.AuthenticationRequestDTO;
import com.gunes.cravings.dto.AuthenticationResponseDTO;
import com.gunes.cravings.dto.RegisterRequestDTO;
import com.gunes.cravings.model.User;
import com.gunes.cravings.repository.UserRepository;
import com.gunes.cravings.exception.EmailAlreadyExistsException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthenticationService {
    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthenticationResponseDTO register(RegisterRequestDTO request) {
        var user = User.builder()
                .firstname(request.getFirstname())
                .lastname(request.getLastname())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole()) // Role is taken from the request
                .build();
        try {
            repository.save(user);
        } catch (DataIntegrityViolationException e) {
            // A more robust check might involve inspecting the SQLException's error code or message
            if (e.getMessage().contains("UNIQUE constraint failed: _user.email")) { // This check is specific to SQLite
                throw new EmailAlreadyExistsException("Email already registered: " + request.getEmail());
            }
            throw e; // Re-throw if it's not the email constraint we're expecting
        }
        var jwtToken = jwtService.generateToken(user);
        return AuthenticationResponseDTO.builder()
                .token(jwtToken)
                .build();
    }

    public AuthenticationResponseDTO authenticate(AuthenticationRequestDTO request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );
        var user = repository.findByEmail(request.getEmail())
                .orElseThrow(); // Consider custom exception here
        var jwtToken = jwtService.generateToken(user);
        return AuthenticationResponseDTO.builder()
                .token(jwtToken)
                .build();
    }
} 