package com.smarthealthdog.backend.repositories;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.smarthealthdog.backend.domain.RefreshToken;


@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

}
