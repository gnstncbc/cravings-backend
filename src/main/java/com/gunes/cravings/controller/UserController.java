package com.gunes.cravings.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gunes.cravings.dto.PlayerDTO;
import com.gunes.cravings.dto.UserDTO;
import com.gunes.cravings.dto.ChangeRoleDTO;
import com.gunes.cravings.service.PlayerService;
import com.gunes.cravings.service.UserService;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;


@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class UserController {

    private final UserService userService;
    @GetMapping
    public ResponseEntity<List<UserDTO>> getAllUsers() {
        List<UserDTO> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    @PostMapping("/change-role")
    public ResponseEntity<UserDTO> changeUserRole(@RequestBody ChangeRoleDTO changeRoleDTO) {
        UserDTO updatedUser = userService.changeUserRole(changeRoleDTO);
        return ResponseEntity.ok(updatedUser);
    }
}
