package com.gunes.cravings.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.gunes.cravings.repository.UserRepository;
import com.gunes.cravings.dto.UserDTO;
import com.gunes.cravings.dto.ChangeRoleDTO;
import com.gunes.cravings.model.Role;
import com.gunes.cravings.model.User;


@Service
public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<UserDTO> getAllUsers() {
        return userRepository.findAllByOrderByFirstnameAsc() // Changed to use findAllByOrderByFirstnameAsc
                .stream()
                .map(this::convertToUserDTO)
                .collect(Collectors.toList());
    }


    private UserDTO convertToUserDTO(User user) {
        UserDTO userDTO = new UserDTO();
        userDTO.setId(user.getId().longValue());
        userDTO.setFirstname(user.getFirstname());
        userDTO.setLastname(user.getLastname());
        userDTO.setEmail(user.getEmail());
        userDTO.setRole(user.getRole().name());
        return userDTO;
    }

    public UserDTO changeUserRole(ChangeRoleDTO changeRoleDTO) {
        User user = userRepository.findByEmail(changeRoleDTO.getEmail()).orElseThrow(() -> new RuntimeException("User not found"));
        user.setRole(Role.valueOf(changeRoleDTO.getRole()));
        userRepository.save(user);
        return convertToUserDTO(user);
    }
}
