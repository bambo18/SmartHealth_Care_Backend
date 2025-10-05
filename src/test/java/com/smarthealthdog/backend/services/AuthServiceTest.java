package com.smarthealthdog.backend.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

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

import com.smarthealthdog.backend.domain.RefreshToken;
import com.smarthealthdog.backend.domain.Role;
import com.smarthealthdog.backend.domain.RoleEnum;
import com.smarthealthdog.backend.domain.User;
import com.smarthealthdog.backend.dto.LoginResponse;
import com.smarthealthdog.backend.dto.UserCreateRequest;
import com.smarthealthdog.backend.exceptions.BadCredentialsException;
import com.smarthealthdog.backend.exceptions.InvalidRequestDataException;
import com.smarthealthdog.backend.exceptions.ResourceNotFoundException;
import com.smarthealthdog.backend.repositories.RefreshTokenRepository;
import com.smarthealthdog.backend.repositories.RoleRepository;
import com.smarthealthdog.backend.repositories.UserRepository;


@TestInstance(Lifecycle.PER_CLASS)
@SpringBootTest
@ActiveProfiles("test")
public class AuthServiceTest {
    @Autowired 
    private AuthService authService;

    @Autowired
    private UserService userService;

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @BeforeAll
    void cleanUp() {
        ReflectionTestUtils.setField(
            refreshTokenService,
            "maxRefreshTokenCount",
            10
        );

        Role unverifiedRole = new Role();
        unverifiedRole.setName(RoleEnum.UNVERIFIED_USER);
        unverifiedRole.setDescription("Unverified user role");
        roleRepository.save(unverifiedRole);

        Role userRole = new Role();
        userRole.setName(RoleEnum.USER);
        userRole.setDescription("Regular user role");
        roleRepository.save(userRole);
    }

    @AfterEach
    void tearDown() {
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    @AfterAll
    void finalCleanUp() {
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
        roleRepository.deleteAll();
    }

    @Test
    void registerUser_shouldReturnCreatedUser() {
        UserCreateRequest createRequest = new UserCreateRequest(
            "testuser",
            "test@example.com",
            "TestPassword123!"
        );

        // Act
        User createdUser = authService.registerUser(createRequest);

        assertTrue(createdUser.getRole().getName() == RoleEnum.UNVERIFIED_USER);
        assertTrue(createdUser.getEmail().equals("test@example.com"));
        assertTrue(createdUser.getNickname().equals("testuser"));

        // Compare the hashed password with the raw password
        assertTrue(createdUser.getPassword() != null);
        assertTrue(createdUser.getPassword().length() > 0);
        assertTrue(userService.checkUserPassword(createdUser, "TestPassword123!"));
    }

    @Test
    void registerUser_ShouldThrowException_WhenNicknameIsInvalid() {
        UserCreateRequest createRequest = new UserCreateRequest(
            "ab", // 너무 짧은 닉네임
            "test@example.com",
            "TestPassword123!"
        );

        // Act & Assert
        assertThrows(InvalidRequestDataException.class, () -> {
            authService.registerUser(createRequest);
        });

        UserCreateRequest createRequest2 = new UserCreateRequest(
            "a".repeat(129), // 너무 긴 닉네임
            "test@example.com",
            "TestPassword123!"
        );

        // Act & Assert
        assertThrows(InvalidRequestDataException.class, () -> {
            authService.registerUser(createRequest2);
        });
    }

    @Test
    void registerUser_ShouldThrowException_WhenEmailAlreadyExists() {
        UserCreateRequest createRequest1 = new UserCreateRequest(
            "testuser1",
            "test@example.com",
            "TestPassword123!"
        );

        // Act & Assert
        authService.registerUser(createRequest1);
        assertThrows(InvalidRequestDataException.class, () -> {
            authService.registerUser(createRequest1);
        });
    }

    @Test
    void registerUser_ShouldThrowException_WhenPasswordIsInvalid() {
        UserCreateRequest createRequest = new UserCreateRequest(
            "testuser",
            "test@example.com",
            "short123!" // 대문자 없음
        );

        // Act & Assert
        assertThrows(InvalidRequestDataException.class, () -> {
            authService.registerUser(createRequest);
        });

        UserCreateRequest createRequest2 = new UserCreateRequest(
            "testuser",
            "test@example.com",
            "SHORT123!" // 소문자 없음
        );

        // Act & Assert
        assertThrows(InvalidRequestDataException.class, () -> {
            authService.registerUser(createRequest2);
        });

        UserCreateRequest createRequest3 = new UserCreateRequest(
            "testuser",
            "test@example.com",
            "NoDigits!" // 숫자 없음
        );

        // Act & Assert
        assertThrows(InvalidRequestDataException.class, () -> {
            authService.registerUser(createRequest3);
        });
    
        UserCreateRequest createRequest4 = new UserCreateRequest(
            "testuser",
            "test@example.com",
            "NoSpecialChar123" // 특수문자 없음
        );

        // Act & Assert
        assertThrows(InvalidRequestDataException.class, () -> {
            authService.registerUser(createRequest4);
        });

        UserCreateRequest createRequest5 = new UserCreateRequest(
            "testuser",
            "test@example.com",
            "A1!" // 너무 짧은 비밀번호
        );

        // Act & Assert
        assertThrows(InvalidRequestDataException.class, () -> {
            authService.registerUser(createRequest5);
        });

        UserCreateRequest createRequest6 = new UserCreateRequest(
            "testuser",
            "test@example.com",
            "A".repeat(257) + "1!" // 너무 긴 비밀번호
        );

        // Act & Assert
        assertThrows(InvalidRequestDataException.class, () -> {
            authService.registerUser(createRequest6);
        });
    }

    @Test
    void verifyEmailToken_ShouldThrowException_WhenUserIsAlreadyVerified() {
        // Arrange
        UserCreateRequest createRequest = new UserCreateRequest(
            "testuser",
            "test@example.com",
            "TestPassword123!"
        );

        // Act
        User user = authService.registerUser(createRequest);
        user.setRole(roleRepository.findByName(RoleEnum.USER).orElseThrow());
        user.setEmailVerificationToken("000000");
        user.setEmailVerificationFailCount(0);
        user.setEmailVerificationExpiry(Instant.now().plusSeconds(3600));
        userRepository.save(user);

        // Assert
        assertThrows(InvalidRequestDataException.class, () -> {
            authService.verifyEmailToken("test@example.com", "000000");
        });
    }

    @Test
    void verifyEmailToken_ShouldThrowException_WhenFailCountExceedsLimit() {
        // Arrange
        UserCreateRequest createRequest = new UserCreateRequest(
            "testuser",
            "test@example.com",
            "TestPassword123!"
        );

        // Act
        User user = authService.registerUser(createRequest);
        user.setEmailVerificationToken("000000");
        user.setEmailVerificationFailCount(5);
        user.setEmailVerificationExpiry(Instant.now().plusSeconds(3600));
        userRepository.save(user);

        // Assert
        assertThrows(InvalidRequestDataException.class, () -> {
            authService.verifyEmailToken("test@example.com", "000000");
        });
    }

    @Test
    void verifyEmailToken_ShouldThrowException_WhenEmailIsNotFound() {
        // Assert
        assertThrows(InvalidRequestDataException.class, () -> {
            authService.verifyEmailToken("nonexistent@example.com", "000000");
        });
    }

    @Test
    void verifyEmailToken_ShouldThrowException_WhenTokenIsExpired() {
        // Arrange
        UserCreateRequest createRequest = new UserCreateRequest(
            "testuser",
            "test@example.com",
            "TestPassword123!"
        );

        // Act
        User user = authService.registerUser(createRequest);
        user.setEmailVerificationToken("000000");
        user.setEmailVerificationFailCount(0);
        user.setEmailVerificationExpiry(Instant.now().minusSeconds(3600));
        userRepository.save(user);

        // Assert
        assertThrows(InvalidRequestDataException.class, () -> {
            authService.verifyEmailToken("test@example.com", "000000");
        });
    }

    @Test
    void verifyEmailToken_ShouldThrowException_WhenTokenDoesNotMatch() {
        // Arrange
        UserCreateRequest createRequest = new UserCreateRequest(
            "testuser",
            "test@example.com",
            "TestPassword123!"
        );

        // Act
        User user = authService.registerUser(createRequest);
        user.setEmailVerificationToken("000000");
        user.setEmailVerificationFailCount(0);
        user.setEmailVerificationExpiry(Instant.now().plusSeconds(3600));
        userRepository.save(user);

        // Assert
        assertThrows(InvalidRequestDataException.class, () -> {
            authService.verifyEmailToken("test@example.com", "111111");
        });

        // Verify that fail count has been incremented
        User updatedUser = userRepository.findById(user.getId()).orElseThrow();
        assertTrue(updatedUser.getEmailVerificationFailCount() == 1);
    }

    @Test
    void verifyEmailToken_ShouldReturnUser_WhenTokenIsValid() {
        // Arrange
        UserCreateRequest createRequest = new UserCreateRequest(
            "testuser",
            "test@example.com",
            "TestPassword123!"
        );

        // Act
        User user = authService.registerUser(createRequest);
        user.setEmailVerificationToken("000000");
        user.setEmailVerificationFailCount(0);
        user.setEmailVerificationExpiry(Instant.now().plusSeconds(3600));
        userRepository.save(user);

        // Assert
        User verifiedUser = authService.verifyEmailToken("test@example.com", "000000");
        assertEquals(user.getId(), verifiedUser.getId());
    }

    @Test
    void activateUser_ShouldThrowException_WhenUserIsAlreadyVerified() {
        // Arrange
        UserCreateRequest createRequest = new UserCreateRequest(
            "testuser",
            "test@example.com",
            "TestPassword123!"
        );

        // Act
        User user = authService.registerUser(createRequest);
        user.setRole(roleRepository.findByName(RoleEnum.USER).orElseThrow());
        assertThrows(InvalidRequestDataException.class, () -> {
            authService.activateUser(user);
        });
    }

    @Test
    void activateUser_ShouldChangeUserRoleAndExpireToken() {
        // Arrange
        UserCreateRequest createRequest = new UserCreateRequest(
            "testuser",
            "test@example.com",
            "TestPassword123!"
        );

        // Act
        User user = authService.registerUser(createRequest);
        user.setRole(roleRepository.findByName(RoleEnum.UNVERIFIED_USER).orElseThrow());
        authService.activateUser(user);
    
        // Assert
        User updatedUser = userRepository.findById(user.getId()).orElseThrow();
        assertEquals(RoleEnum.USER, updatedUser.getRole().getName());
        assertTrue(updatedUser.getEmailVerificationExpiry() != null);
        assertTrue(Instant.now().isAfter(updatedUser.getEmailVerificationExpiry()));
        assertTrue(updatedUser.getEmailVerificationToken() == null);
        assertTrue(updatedUser.getEmailVerificationFailCount() == 0);
    }

    @Test
    void generateTokens_shouldThrowException_whenUserNotFound() {
        // Arrange
        Long nonExistentUserId = 999L;
        assertThrows(ResourceNotFoundException.class, () -> {
            authService.generateTokens(nonExistentUserId);
        });
    }

    @Test
    void generateTokens_ShouldReturnTokens_whenUserExists() {
        // Arrange
        UserCreateRequest createRequest = new UserCreateRequest(
            "testuser",
            "test@example.com",
            "TestPassword123!"
        );

        // Act
        User user = authService.registerUser(createRequest);
        user.setRole(roleRepository.findByName(RoleEnum.USER).orElseThrow());
        userRepository.save(user);

        // Assert
        LoginResponse loginResponse = authService.generateTokens(user.getId());
        assertTrue(loginResponse.accessToken() != null && loginResponse.accessToken().length() > 0);
        assertTrue(loginResponse.refreshToken() != null && loginResponse.refreshToken().length() > 0);
        assertTrue(loginResponse.expiration() != null && loginResponse.expiration().length() > 0);
    }

    @Test
    void generateTokens_ShouldDeleteExpiredRefreshTokens() {
        // Arrange
        UserCreateRequest createRequest = new UserCreateRequest(
            "testuser",
            "test@example.com",
            "TestPassword123!"
        );

        // Act
        User user = authService.registerUser(createRequest);
        user.setRole(roleRepository.findByName(RoleEnum.USER).orElseThrow());
        userRepository.save(user);

        // Create an expired refresh token
        RefreshToken expiredToken = new RefreshToken();
        expiredToken.setUser(user);
        expiredToken.setExpiresAt(Instant.now().minusSeconds(3600)); // 이미 만료된 토큰
        expiredToken.setId(UUID.randomUUID());
        refreshTokenRepository.save(expiredToken);

        assertTrue(refreshTokenRepository.findByUser(user).size() == 1);

        // Generate new tokens, which should trigger deletion of expired tokens
        authService.generateTokens(user.getId());
        assertTrue(refreshTokenRepository.findByUser(user).size() == 1);
        assertTrue(refreshTokenRepository.findByUser(user).get(0).getId() != expiredToken.getId());
    }

    @Test
    void generateToken_ShouldEnforceMaxRefreshTokenCount() {
        // Arrange
        UserCreateRequest createRequest = new UserCreateRequest(
            "testuser",
            "test@example.com",
            "TestPassword123!"
        );

        // Act
        User user = authService.registerUser(createRequest);
        user.setRole(roleRepository.findByName(RoleEnum.USER).orElseThrow());
        userRepository.save(user);

        // Generate multiple tokens to exceed the max count
        for (int i = 0; i < 10; i++) {
            refreshTokenService.generateRefreshToken(user);
        }

        // Assert
        List<RefreshToken> tokens = refreshTokenRepository.findByUser(user);
        assertEquals(10, tokens.size());
        
        // Generate one more token to trigger enforcement
        authService.generateTokens(user.getId());

        List<RefreshToken> updatedTokens = refreshTokenRepository.findByUser(user);
        assertEquals(10, updatedTokens.size());
        // Compare both lists by creating two sets of token IDs
        Set<UUID> originalTokenIds = tokens.stream()
            .map(RefreshToken::getId)
            .collect(Collectors.toSet());

        Set<UUID> updatedTokenIds = updatedTokens.stream()
            .map(RefreshToken::getId)
            .collect(Collectors.toSet());
        
        // Ensure that at least one of the original tokens has been removed
        originalTokenIds.retainAll(updatedTokenIds);
        assertTrue(originalTokenIds.size() == 9);
    }

    @Test
    void refreshAccessToken_ShouldThrowException_WhenRefreshTokenIsNullOrEmpty() {
        assertThrows(BadCredentialsException.class, () -> {
            authService.refreshAccessToken(null);
        });

        assertThrows(BadCredentialsException.class, () -> {
            authService.refreshAccessToken("");
        });
    }

    @Test
    void refreshAccessToken_ShouldThrowException_WhenRefreshTokenIsInvalid() {
        assertThrows(BadCredentialsException.class, () -> {
            authService.refreshAccessToken("invalid.token.here");
        });
    }

    @Test
    void refreshAccessToken_ShouldReturnNewTokens_WhenRefreshTokenIsValid() {
        // Arrange
        UserCreateRequest createRequest = new UserCreateRequest(
            "testuser",
            "test@example.com",
            "TestPassword123!"
        );

        // Act
        User user = authService.registerUser(createRequest);
        user.setRole(roleRepository.findByName(RoleEnum.USER).orElseThrow());
        userRepository.save(user);

        String refreshToken = refreshTokenService.generateRefreshToken(user);
        assertTrue(refreshToken != null && refreshToken.length() > 0);

        // Assert
        LoginResponse loginResponse = authService.refreshAccessToken(refreshToken);
        assertTrue(loginResponse.accessToken() != null && loginResponse.accessToken().length() > 0);
        assertTrue(loginResponse.refreshToken() != null && loginResponse.refreshToken().length() > 0);
        assertTrue(loginResponse.expiration() != null && loginResponse.expiration().length() > 0);
        assertTrue(!loginResponse.refreshToken().equals(refreshToken));
    }
}