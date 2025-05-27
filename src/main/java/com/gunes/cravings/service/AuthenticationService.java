package com.gunes.cravings.service;

import com.gunes.cravings.config.JwtService;
import com.gunes.cravings.dto.AuthenticationRequestDTO;
import com.gunes.cravings.dto.AuthenticationResponseDTO;
import com.gunes.cravings.dto.RegisterRequestDTO;
import com.gunes.cravings.model.User;
import com.gunes.cravings.repository.UserRepository;
import com.gunes.cravings.exception.EmailAlreadyExistsException;
// import com.gunes.cravings.exception.InvalidReferralCodeException; // Already handled by ReferralCodeService
import com.gunes.cravings.model.ReferralCode; // Import ReferralCode
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // Ensure transactional behavior

@Service
@RequiredArgsConstructor
public class AuthenticationService {
    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final ReferralCodeService referralCodeService; // Inject ReferralCodeService

    @Transactional // Make the whole registration process transactional
    public AuthenticationResponseDTO register(RegisterRequestDTO request) {
        // Validate and mark referral code as used (this will throw if invalid/used/inactive)
        // This is done first to ensure the code is valid before attempting user creation.
        // Note: The actual marking as 'used' and linking to the user happens after user is saved.
        
        // Check if email already exists
        if (repository.findByEmail(request.getEmail()).isPresent()) {
            throw new EmailAlreadyExistsException("Email already registered: " + request.getEmail());
        }

        var user = User.builder()
                .firstname(request.getFirstname())
                .lastname(request.getLastname())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .build();
        
        User savedUser;
        try {
            savedUser = repository.save(user);
        } catch (DataIntegrityViolationException e) {
            // This catch might be redundant if the above email check is reliable,
            // but good for catching other potential integrity issues if any.
            if (e.getMessage() != null && e.getMessage().contains("UNIQUE constraint failed: _user.email")) { 
                throw new EmailAlreadyExistsException("Email already registered: " + request.getEmail());
            }
            throw e; 
        }

        // Now that user is saved and has an ID, associate the referral code
        referralCodeService.findByCodeAndMarkAsUsed(request.getReferralCode(), savedUser);

        var jwtToken = jwtService.generateToken(savedUser); // Use savedUser for token generation
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