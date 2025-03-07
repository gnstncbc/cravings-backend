package com.gunes.cravings.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.Optional;
import com.gunes.cravings.model.User;
import com.gunes.cravings.repository.UserRepository;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Autowired
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User createUser(String username, String password, String email) {
        
        if (!isUserExits(username)){
            String hashedPassword = passwordEncoder.encode(password);
            User user = new User(username, hashedPassword, email);
            return userRepository.save(user);
        } else {
            return null;
        }
    }

    public Optional<User> findUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public void deleteUser(User user) {
        userRepository.delete(user);
    }

    public boolean passwordsMatch(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }

    private boolean isUserExits(String username) {
        if (userRepository.findByUsername(username).isPresent()) {
            return true;
        }
        return false;
    }
}

