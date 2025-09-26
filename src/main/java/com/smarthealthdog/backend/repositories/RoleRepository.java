package com.smarthealthdog.backend.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.smarthealthdog.backend.domain.Role;

import java.util.Optional;

@Repository  // Optional, but recommended for clarity
public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(String name);
}
