package com.gunes.cravings.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.gunes.cravings.model.User;
import com.gunes.cravings.service.UserService;

import java.util.Optional;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<User> registerUser(@RequestBody User user) {
        User savedUser = userService.createUser(user.getUsername(), user.getPassword(), user.getEmail());
        return ResponseEntity.ok(savedUser);
    }

    @GetMapping("/{username}")
    public ResponseEntity<User> getUser(@PathVariable String username) {
        Optional<User> user = userService.findUserByUsername(username);
        return user.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{username}")
    public ResponseEntity<Void> deleteUser(@PathVariable String username) {
        Optional<User> user = userService.findUserByUsername(username);
        if (user.isPresent()) {
            userService.deleteUser(user.get());
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping("/login")
    public ResponseEntity<User> loginUser(@RequestBody User user) {
        Optional<User> userOptional = userService.findUserByUsername(user.getUsername());
        if (userOptional.isPresent()) {
            User userFromDb = userOptional.get();
            if (userService.passwordsMatch(user.getPassword(), userFromDb.getPassword())) {
                return ResponseEntity.ok(userFromDb);
            }
        }
        return ResponseEntity.notFound().build();
    }
}

