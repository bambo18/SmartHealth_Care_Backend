package com.smarthealthdog.backend.repositories;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import com.smarthealthdog.backend.domain.RefreshToken;
import com.smarthealthdog.backend.domain.Role;
import com.smarthealthdog.backend.domain.RoleEnum;
import com.smarthealthdog.backend.domain.User;

@DataJpaTest
@ActiveProfiles("test")
class RefreshTokenRepositoryTest {
    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        Role role = new Role();
        role.setName(RoleEnum.UNVERIFIED_USER);
        role.setDescription("Role for unverified users");
        roleRepository.save(role);

        testUser = new User();
        testUser.setNickname("testuser");
        testUser.setEmail("testuser@example.com");
        testUser.setPassword("hashedpassword");
        testUser.setRole(role);

        userRepository.save(testUser);
    }

    @Test
    void deleteByUser_ShouldDeleteTokens_WhenUserExists() {
        RefreshToken token1 = new RefreshToken();
        token1.setId(java.util.UUID.randomUUID());
        token1.setUser(testUser);
        token1.setExpiresAt(java.time.Instant.now().plusSeconds(3600));

        refreshTokenRepository.save(token1);

        Long tokenCount = refreshTokenRepository.count();
        assertTrue(tokenCount == 1);

        refreshTokenRepository.deleteByUser(testUser);
        tokenCount = refreshTokenRepository.count();
        assertTrue(tokenCount == 0);
    }
}
