package com.smarthealthdog.backend.services;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
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
import com.smarthealthdog.backend.dto.UserCreateRequest;
import com.smarthealthdog.backend.exceptions.BadCredentialsException;
import com.smarthealthdog.backend.repositories.RefreshTokenRepository;
import com.smarthealthdog.backend.repositories.RoleRepository;
import com.smarthealthdog.backend.utils.JWTUtils;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;


@TestInstance(Lifecycle.PER_CLASS)
@SpringBootTest
@ActiveProfiles("test")
public class RefreshTokenServiceTest {
    @Autowired
    private RefreshTokenService refreshTokenService; 

    @Autowired
    private AuthService authService;

    @Autowired
    private UserService userService;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private JWTUtils jwtUtils;

    SecretKey key;

    @BeforeAll
    void setUp() {
        roleRepository.deleteAll();
        Role unverifiedRole = new Role();
        unverifiedRole.setName(RoleEnum.UNVERIFIED_USER);
        unverifiedRole.setDescription("Unverified User");

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

    @BeforeEach
    void setUpEach() {
        refreshTokenRepository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        refreshTokenRepository.deleteAll();
    }

    @AfterAll
    void tearDownAll() {
        refreshTokenRepository.deleteAll();
    }

    @Test
    void deleteUserRefreshTokens_ShouldDeleteAllTokensForUser() {
        Optional<User> userOpt = userService.getUserByEmail("testuser@example.com");
        assertTrue(userOpt.isPresent());

        User user = userOpt.get();
        String token = refreshTokenService.generateRefreshToken(user);
        refreshTokenService.validateRefreshToken(token);

        refreshTokenService.deleteUserRefreshTokens(user);
        assert(refreshTokenRepository.findByUser(user).isEmpty());
    }

    @Test
    void generateRefreshToken_ShouldCreateAndStoreRefreshToken() {
        Optional<User> userOpt = userService.getUserByEmail("testuser@example.com");
        assertTrue(userOpt.isPresent());

        User user = userOpt.get();
        String token = refreshTokenService.generateRefreshToken(user);
        assertTrue(token != null);
        assertTrue(refreshTokenRepository.findByUser(user).size() == 1);
    }

    @Test
    void generateRefreshToken_ShouldThrowException_WhenUserIsNull() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            refreshTokenService.generateRefreshToken(null);
        });

        assertTrue(exception.getMessage().contains("User or User ID cannot be null"));
    }

    @Test
    void validateRefreshToken_ShouldThrowException_WhenTokenIsNullOrEmpty() {
        assertThrows(BadCredentialsException.class, () -> {
            refreshTokenService.validateRefreshToken(null);
        });

        assertThrows(BadCredentialsException.class, () -> {
            refreshTokenService.validateRefreshToken("");
        });
    }

    @Test
    void validateRefreshToken_ShouldThrowException_WhenTokenIsInvalid() {
        assertThrows(BadCredentialsException.class, () -> {
            refreshTokenService.validateRefreshToken("invalid.token.here");
        });
    }

    @Test
    void validateRefreshToken_ShouldThrowException_WhenTokenHasEmptyClaims() {
        String emptyClaimsToken = Jwts.builder()
                                      .signWith(key)
                                      .compact();

        assertThrows(BadCredentialsException.class, () -> {
            refreshTokenService.validateRefreshToken(emptyClaimsToken);
        });
    }

    @Test
    void validateRefreshToken_ShouldThrowException_WhenSubjectIsMissing() {
        String tokenWithoutSubject = Jwts.builder()
                                         .id("asdf")
                                         .signWith(key)
                                         .compact();

        assertThrows(BadCredentialsException.class, () -> {
            refreshTokenService.validateRefreshToken(tokenWithoutSubject);
        });

        String tokenWithEmptySubject = Jwts.builder()
                                          .id("asdf")
                                          .subject("")
                                          .signWith(key)
                                          .compact();

        assertThrows(BadCredentialsException.class, () -> {
            refreshTokenService.validateRefreshToken(tokenWithEmptySubject);
        });
    }

    @Test
    void validateRefreshToken_ShouldThrowException_WhenUserIdIsNotNumber() {
        String tokenWithNonNumericSubject = Jwts.builder()
                                               .id(UUID.randomUUID().toString())
                                               .subject("not-a-number")
                                               .signWith(key)
                                               .compact();

        assertThrows(BadCredentialsException.class, () -> {
            refreshTokenService.validateRefreshToken(tokenWithNonNumericSubject);
        });
    }

    @Test
    void validateRefreshToken_ShouldThrowException_WhenTokenIdIsMissing() {
        String tokenWithoutId = Jwts.builder()
                                    .subject("1234")
                                    .signWith(key)
                                    .compact();

        assertThrows(BadCredentialsException.class, () -> {
            refreshTokenService.validateRefreshToken(tokenWithoutId);
        });
    }

    @Test
    void validateRefreshToken_ShouldThrowException_WhenTokenIdIsNotUUID() {
        String tokenWithInvalidId = Jwts.builder()
                                        .id("not-a-uuid")
                                        .subject("1234")
                                        .signWith(key)
                                        .compact();

        assertThrows(BadCredentialsException.class, () -> {
            refreshTokenService.validateRefreshToken(tokenWithInvalidId);
        });
    }

    @Test
    void validateRefreshToken_ShouldThrowException_WhenTokenIdDoesNotExistInDatabase() {
        UUID randomUuid = UUID.randomUUID();
        String tokenWithNonexistentId = Jwts.builder()
                                            .id(randomUuid.toString())
                                            .subject("1234")
                                            .signWith(key)
                                            .compact();

        assertThrows(BadCredentialsException.class, () -> {
            refreshTokenService.validateRefreshToken(tokenWithNonexistentId);
        });
    }

    @Test
    void validateRefreshToken_ShouldThrowException_WhenTokenIsExpired() {
        String expiredToken = Jwts.builder()
                                  .id(UUID.randomUUID().toString())
                                  .subject("1234")
                                  .expiration(new java.util.Date(System.currentTimeMillis() - 10000)) // Set to 10 second in the past
                                  .signWith(key)
                                  .compact();

        assertThrows(BadCredentialsException.class, () -> {
            refreshTokenService.validateRefreshToken(expiredToken);
        });
    }

    @Test
    void revokeRefreshToken_ShouldDeleteTokenFromDatabase() {
        Optional<User> userOpt = userService.getUserByEmail("testuser@example.com");
        assertTrue(userOpt.isPresent());

        // 토큰 생성
        User user = userOpt.get();
        String token = refreshTokenService.generateRefreshToken(user);

        // 토큰이 데이터베이스에 저장되었는지 확인
        List<RefreshToken> userTokens = refreshTokenRepository.findByUser(user);
        assertTrue(userTokens.size() == 1);

        // 토큰 폐기
        refreshTokenService.revokeRefreshToken(token);
        assertTrue(refreshTokenRepository.findByUser(user).isEmpty());

        // 토큰 폐기 후, 유효성 검사 시도 시 예외 발생
        assertThrows(BadCredentialsException.class, () -> {
            refreshTokenService.validateRefreshToken(token);
        });
    }

    @Test
    void revokeRefreshToken_ShouldThrowException_WhenTokenIsNullOrEmpty() {
        assertThrows(BadCredentialsException.class, () -> {
            refreshTokenService.revokeRefreshToken(null);
        });

        assertThrows(BadCredentialsException.class, () -> {
            refreshTokenService.revokeRefreshToken("");
        });
    }

    @Test
    void revokeRefreshToken_ShouldThrowException_WhenTokenIsInvalid() {
        assertThrows(BadCredentialsException.class, () -> {
            refreshTokenService.revokeRefreshToken("invalid.token.here");
        });
    }

    @Test
    void revokeRefreshToken_ShouldThrowException_WhenTokenHasEmptyClaims() {
        String emptyClaimsToken = Jwts.builder()
                                      .signWith(key)
                                      .compact();

        assertThrows(BadCredentialsException.class, () -> {
            refreshTokenService.revokeRefreshToken(emptyClaimsToken);
        });
    }

    @Test
    void revokeRefreshToken_ShouldThrowException_WhenTokenIdIsMissing() {
        String tokenWithoutId = Jwts.builder()
                                    .subject("some-user-id")
                                    .signWith(key)
                                    .compact();

        assertThrows(BadCredentialsException.class, () -> {
            refreshTokenService.revokeRefreshToken(tokenWithoutId);
        });
    }

    @Test
    void revokeRefreshToken_ShouldThrowException_WhenTokenIdIsNotUUID() {
        String tokenWithInvalidId = Jwts.builder()
                                        .id("not-a-uuid")
                                        .subject("some-user-id")
                                        .signWith(key)
                                        .compact();

        assertThrows(BadCredentialsException.class, () -> {
            refreshTokenService.revokeRefreshToken(tokenWithInvalidId);
        });
    }

    @Test
    void validateAccessToken_ShouldThrowException_WhenTokenIsNullOrEmpty() {
        assertThrows(BadCredentialsException.class, () -> {
            refreshTokenService.validateAccessToken(null);
        });

        assertThrows(BadCredentialsException.class, () -> {
            refreshTokenService.validateAccessToken("");
        });
    }

    @Test
    void validateAccessToken_ShouldThrowException_WhenTokenIsInvalid() {
        assertThrows(BadCredentialsException.class, () -> {
            refreshTokenService.validateAccessToken("invalid.token.here");
        });
    }

    @Test
    void validateAccessToken_ShouldThrowException_WhenTokenHasEmptyClaims() {
        String emptyClaimsToken = Jwts.builder()
                                      .signWith(key)
                                      .compact();

        assertThrows(BadCredentialsException.class, () -> {
            refreshTokenService.validateAccessToken(emptyClaimsToken);
        });
    }

    @Test
    void validateAccessToken_ShouldThrowException_WhenSubjectIsMissing() {
        String tokenWithoutSubject = Jwts.builder()
                                         .id("asdf")
                                         .signWith(key)
                                         .compact();

        assertThrows(BadCredentialsException.class, () -> {
            refreshTokenService.validateAccessToken(tokenWithoutSubject);
        });

        String tokenWithEmptySubject = Jwts.builder()
                                          .id("asdf")
                                          .subject("")
                                          .signWith(key)
                                          .compact();

        assertThrows(BadCredentialsException.class, () -> {
            refreshTokenService.validateAccessToken(tokenWithEmptySubject);
        });
    }

    @Test
    void validateAccessToken_ShouldThrowException_WhenUserIdIsNotNumber() {
        String tokenWithNonNumericSubject = Jwts.builder()
                                               .subject("not-a-number")
                                               .signWith(key)
                                               .compact();

        assertThrows(BadCredentialsException.class, () -> {
            refreshTokenService.validateAccessToken(tokenWithNonNumericSubject);
        });
    }

    @Test
    void validateAccessToken_ShouldThrowException_WhenTokenIsExpired() {
        String expiredToken = Jwts.builder()
                                  .subject("1234")
                                  .expiration(new java.util.Date(System.currentTimeMillis() - 10000)) // Set to 10 second in the past
                                  .signWith(key)
                                  .compact();

        assertThrows(BadCredentialsException.class, () -> {
            refreshTokenService.validateAccessToken(expiredToken);
        });
    }
}