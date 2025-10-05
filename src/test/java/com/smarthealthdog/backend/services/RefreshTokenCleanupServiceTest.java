package com.smarthealthdog.backend.services;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import com.smarthealthdog.backend.domain.Permission;
import com.smarthealthdog.backend.domain.PermissionEnum;
import com.smarthealthdog.backend.domain.RefreshToken;
import com.smarthealthdog.backend.domain.Role;
import com.smarthealthdog.backend.domain.RoleEnum;
import com.smarthealthdog.backend.domain.User;
import com.smarthealthdog.backend.dto.UserCreateRequest;
import com.smarthealthdog.backend.repositories.PermissionRepository;
import com.smarthealthdog.backend.repositories.RefreshTokenRepository;
import com.smarthealthdog.backend.repositories.RoleRepository;
import com.smarthealthdog.backend.repositories.UserRepository;
import com.smarthealthdog.backend.utils.JWTUtils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@TestInstance(Lifecycle.PER_CLASS)
@SpringBootTest
@ActiveProfiles("test")
public class RefreshTokenCleanupServiceTest {
    @Autowired
    private RefreshTokenCleanupService refreshTokenCleanupService;

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private UserService userService;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private AuthService authService;

    @Autowired
    private JWTUtils jwtUtils;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PermissionRepository permissionRepository;

    SecretKey key;

    @BeforeAll
    void setUp() {
        Permission loginPermission = new Permission();
        loginPermission.setName(PermissionEnum.CAN_LOGIN);
        loginPermission.setDescription("Can log in");
        permissionRepository.save(loginPermission);

        Role unverifiedRole = new Role();
        unverifiedRole.setName(RoleEnum.UNVERIFIED_USER);
        unverifiedRole.setDescription("Unverified User");
        unverifiedRole.setPermissions(Set.of(loginPermission));
        roleRepository.save(unverifiedRole);

        // Create a new user before each test
        UserCreateRequest request = new UserCreateRequest(
            "testuser",
            "testuser@example.com",
            "Password123!"
        );
        User user = authService.registerUser(request);

        // Ensure the user is created successfully
        assertTrue(user != null);
        assertTrue(user.getId() != null);

        refreshTokenRepository.deleteAll();
        ReflectionTestUtils.setField(
            refreshTokenService, 
            "refreshTokenExpirationInDays", 
            7L // Use 'L' for a Long value
        );

        // Initialize JWTUtils
        // generate a 64 hex character secret key for HS256
        key = Jwts.SIG.HS256.key().build();
        ReflectionTestUtils.setField(
            jwtUtils, 
            "key",
            Keys.hmacShaKeyFor(key.getEncoded())
        );
    }

    @AfterEach
    void cleanUp() {
        refreshTokenRepository.deleteAll();
    }

    @AfterAll
    void tearDown() {
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
        roleRepository.deleteAll();
        permissionRepository.deleteAll();
    }

    @Test
    void deleteRefreshTokensById_ShouldDeleteTokens() {
        Optional<User> userOpt = userService.getUserByEmail("testuser@example.com");
        assertTrue(userOpt.isPresent());
        User user = userOpt.get();
        String token = refreshTokenService.generateRefreshToken(user);
        refreshTokenService.validateRefreshToken(token);

        assertTrue(refreshTokenRepository.findByUser(user).size() == 1);

        Jws<Claims> claims = refreshTokenService.getClaimsFromToken(token);
        UUID tokenId = UUID.fromString(claims.getPayload().getId());
        refreshTokenCleanupService.deleteRefreshTokensById(tokenId);
        assertTrue(refreshTokenRepository.findByUser(user).isEmpty());
    }

    @Test
    void deleteRefreshTokensByIdInNewTransaction_ShouldDeleteTokens() {
        Optional<User> userOpt = userService.getUserByEmail("testuser@example.com");
        assertTrue(userOpt.isPresent());

        User user = userOpt.get();

        String token = refreshTokenService.generateRefreshToken(user);
        refreshTokenService.validateRefreshToken(token);
        assertTrue(refreshTokenRepository.findByUser(user).size() == 1);

        Jws<Claims> claims = refreshTokenService.getClaimsFromToken(token);
        UUID tokenId = UUID.fromString(claims.getPayload().getId());

        refreshTokenCleanupService.deleteRefreshTokensByIdInNewTransaction(tokenId);
        assertTrue(refreshTokenRepository.findByUser(user).isEmpty());
    }

    @Test
    void deleteUserRefreshTokensIfExpired_ShouldDeleteExpiredTokens() throws InterruptedException {
        Optional<User> userOpt = userService.getUserByEmail("testuser@example.com");
        assertTrue(userOpt.isPresent());

        Instant now = Instant.now().minus(8, ChronoUnit.DAYS);
        Date issuedAt = Date.from(now);

        User user = userOpt.get();
        UUID jti = UUID.randomUUID();
        jwtUtils.generateRefreshToken(user.getId().toString(), jti, issuedAt);

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setId(jti);
        refreshToken.setUser(user);
        refreshToken.setExpiresAt(now.plus(7, ChronoUnit.DAYS));
        refreshTokenRepository.save(refreshToken);

        assertTrue(refreshTokenRepository.findByUser(user).size() == 1);

        refreshTokenCleanupService.deleteUserRefreshTokensIfExpired(user);
        assertTrue(refreshTokenRepository.findByUser(user).isEmpty());
    }
}
