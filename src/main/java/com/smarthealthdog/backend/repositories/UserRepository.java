package com.smarthealthdog.backend.repositories;

import com.smarthealthdog.backend.domain.User;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository  // Optional, but recommended for clarity
public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByEmail(String email);
    boolean existsByNickname(String nickname);
    boolean existsById(Long id);

    Optional<User> findByEmail(String email);
    Optional<User> findByNickname(String nickname);
}
