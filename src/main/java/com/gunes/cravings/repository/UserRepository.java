package com.gunes.cravings.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import com.gunes.cravings.model.User;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
}
