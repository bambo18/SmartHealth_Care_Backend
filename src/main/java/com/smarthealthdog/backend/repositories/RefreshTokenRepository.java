package com.smarthealthdog.backend.repositories;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.smarthealthdog.backend.domain.RefreshToken;
import com.smarthealthdog.backend.domain.User;


@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
    public void deleteByUser(User user);

    public List<RefreshToken> findByUser(User user);
}
