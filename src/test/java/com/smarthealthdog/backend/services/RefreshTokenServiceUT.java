package com.smarthealthdog.backend.services;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.smarthealthdog.backend.domain.User;
import com.smarthealthdog.backend.exceptions.BadCredentialsException;
import com.smarthealthdog.backend.repositories.RefreshTokenRepository;
import com.smarthealthdog.backend.utils.JWTUtils;
import com.smarthealthdog.backend.validation.ErrorCode;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;

@ExtendWith(MockitoExtension.class)
public class RefreshTokenServiceUT {
    // Add unit tests for RefreshTokenService here
    @Mock  
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private JWTUtils jwtUtils;

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    private final String MOCK_REFRESH_TOKEN = "mockRefreshToken";
    private final UUID uuid = UUID.fromString("00000000-0000-0000-0000-000000000000");

    private User mockUser = mock(User.class);

    @SuppressWarnings("unchecked")
    private Jws<Claims> mockJws = mock(Jws.class);
    private Claims mockClaims = mock(Claims.class);

    @Test
    void deleteUserRefreshTokens_ShouldCallDeleteByUser() {
        // ACT
        refreshTokenService.deleteUserRefreshTokens(mockUser);

        // ASSERT
        verify(refreshTokenRepository).deleteByUser(mockUser);
    }

    @Test
    void getClaimsFromToken_ShouldReturnClaims_WhenTokenIsValid() {
        // ARRANGE
        when(jwtUtils.getAllClaimsFromToken(MOCK_REFRESH_TOKEN)).thenReturn(mockJws);

        // ACT
        Jws<Claims> claims = refreshTokenService.getClaimsFromToken(MOCK_REFRESH_TOKEN);

        // ASSERT
        assertTrue(claims != null);
    }

    @Test
    void generateAccessToken_ShouldReturnNewAccessToken_WhenRefreshTokenIsValid() {
        // ARRANGE
        when(jwtUtils.getAllClaimsFromToken(MOCK_REFRESH_TOKEN)).thenReturn(mockJws);
        when(mockJws.getPayload()).thenReturn(mockClaims);
        when(mockClaims.getId()).thenReturn("00000000-0000-0000-0000-000000000000");
        when(refreshTokenRepository.existsById(uuid)).thenReturn(true);
        when(mockClaims.getSubject()).thenReturn("1234");
        when(mockClaims.getExpiration()).thenReturn(Date.from(Instant.now().plus(1, ChronoUnit.DAYS)));
        when(jwtUtils.generateAccessToken(eq("1234"), any(Date.class))).thenReturn("newAccessToken");

        String newAccessToken = refreshTokenService.generateAccessToken(MOCK_REFRESH_TOKEN);
        assertTrue(newAccessToken != null && !newAccessToken.isEmpty());
    }

    @Test
    void validateAccessToken_ShouldThrowBadCredentialsException_WhenAccessTokenIsNull() {
        // ARRANGE
        when(jwtUtils.getAllClaimsFromToken(MOCK_REFRESH_TOKEN)).thenReturn(null);

        // ACT & ASSERT
        BadCredentialsException thrown = assertThrows(BadCredentialsException.class, () -> {
            refreshTokenService.validateAccessToken(MOCK_REFRESH_TOKEN);
        });

        assertTrue(thrown.getErrorCode() == ErrorCode.INVALID_JWT);
    }

    @Test
    void validateAccessToken_ShouldThrowBadCredentialsException_WhenJTIIsNull() {
        // ARRANGE
        when(jwtUtils.getAllClaimsFromToken(MOCK_REFRESH_TOKEN)).thenReturn(mockJws);
        when(mockJws.getPayload()).thenReturn(mockClaims);
        when(mockClaims.getSubject()).thenReturn(null);

        // ACT & ASSERT
        BadCredentialsException thrown = assertThrows(BadCredentialsException.class, () -> {
            refreshTokenService.validateAccessToken(MOCK_REFRESH_TOKEN);
        });

        assertTrue(thrown.getErrorCode() == ErrorCode.INVALID_JWT);
    }

    @Test
    void validateAccessToken_ShouldThrowBadCredentialsException_WhenJTIIsEmpty() {
        // ARRANGE
        when(jwtUtils.getAllClaimsFromToken(MOCK_REFRESH_TOKEN)).thenReturn(mockJws);
        when(mockJws.getPayload()).thenReturn(mockClaims);
        when(mockClaims.getSubject()).thenReturn("");

        // ACT & ASSERT
        BadCredentialsException thrown = assertThrows(BadCredentialsException.class, () -> {
            refreshTokenService.validateAccessToken(MOCK_REFRESH_TOKEN);
        });

        assertTrue(thrown.getErrorCode() == ErrorCode.INVALID_JWT);
    }

    @Test
    void validateAccessToken_ShouldThrowBadCredentialsException_WhenTokenIsExpired() {
        // ARRANGE
        when(jwtUtils.getAllClaimsFromToken(MOCK_REFRESH_TOKEN)).thenReturn(mockJws);
        when(mockJws.getPayload()).thenReturn(mockClaims);
        when(mockClaims.getSubject()).thenReturn("userId");
        when(mockClaims.getExpiration()).thenReturn(Date.from(Instant.now().minus(1, ChronoUnit.DAYS)));

        // ACT & ASSERT
        BadCredentialsException thrown = assertThrows(BadCredentialsException.class, () -> {
            refreshTokenService.validateAccessToken(MOCK_REFRESH_TOKEN);
        });

        assertTrue(thrown.getErrorCode() == ErrorCode.INVALID_JWT);
    }

    @Test
    void generateRefreshToken_ShouldReturnNewRefreshToken() {
        User mockUser = mock(User.class);

        ReflectionTestUtils.setField(
            refreshTokenService, 
            "refreshTokenExpirationInDays", 
            7L // Use 'L' for a Long value
        );

        when(refreshTokenRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);
        when(jwtUtils.generateRefreshToken(
            any(String.class), 
            any(UUID.class), 
            any(Date.class)
        )).thenReturn("newRefreshToken");

        String newRefreshToken = refreshTokenService.generateRefreshToken(mockUser);
        assertTrue(newRefreshToken != null && !newRefreshToken.isEmpty());
    }

    @Test
    void validateRefreshToken_ShouldThrowBadCredentialsException_WhenRefreshTokenIsExpired() {
        // ARRANGE
        when(jwtUtils.getAllClaimsFromToken(MOCK_REFRESH_TOKEN)).thenReturn(mockJws);
        when(mockJws.getPayload()).thenReturn(mockClaims);
        when(mockClaims.getSubject()).thenReturn("1234");
        when(mockClaims.getId()).thenReturn("00000000-0000-0000-0000-000000000000");
        when(refreshTokenRepository.existsById(uuid)).thenReturn(true);
        when(mockClaims.getExpiration()).thenReturn(Date.from(Instant.now().minus(1, ChronoUnit.DAYS)));

        // ACT & ASSERT
        BadCredentialsException thrown = assertThrows(BadCredentialsException.class, () -> {
            refreshTokenService.validateRefreshToken(MOCK_REFRESH_TOKEN);
        });

        assertTrue(thrown.getErrorCode() == ErrorCode.INVALID_JWT);
    }

    @Test
    void validateRefreshToken_ShouldThrowBadCredentialsException_WhenJTIIsNull() {
        // ARRANGE
        when(jwtUtils.getAllClaimsFromToken(MOCK_REFRESH_TOKEN)).thenReturn(mockJws);
        when(mockJws.getPayload()).thenReturn(mockClaims);
        when(mockClaims.getId()).thenReturn(null);

        // ACT & ASSERT
        BadCredentialsException thrown = assertThrows(BadCredentialsException.class, () -> {
            refreshTokenService.validateRefreshToken(MOCK_REFRESH_TOKEN);
        });

        assertTrue(thrown.getErrorCode() == ErrorCode.INVALID_JWT);
    }

    @Test
    void validateRefreshToken_ShouldThrowBadCredentialsException_WhenJTIIsInvalidUUID() {
        // ARRANGE
        when(jwtUtils.getAllClaimsFromToken(MOCK_REFRESH_TOKEN)).thenReturn(mockJws);
        when(mockJws.getPayload()).thenReturn(mockClaims);
        when(mockClaims.getId()).thenReturn("invalid-uuid");

        // ACT & ASSERT
        BadCredentialsException thrown = assertThrows(BadCredentialsException.class, () -> {
            refreshTokenService.validateRefreshToken(MOCK_REFRESH_TOKEN);
        });

        assertTrue(thrown.getErrorCode() == ErrorCode.INVALID_JWT);
    }

    @Test
    void validateRefreshToken_ShouldThrowBadCredentialsException_WhenTokenNotInDatabase() {
        // ARRANGE
        when(jwtUtils.getAllClaimsFromToken(MOCK_REFRESH_TOKEN)).thenReturn(mockJws);
        when(mockJws.getPayload()).thenReturn(mockClaims);
        when(mockClaims.getSubject()).thenReturn("1234");
        when(mockClaims.getId()).thenReturn("00000000-0000-0000-0000-000000000000");
        when(refreshTokenRepository.existsById(uuid)).thenReturn(false);

        // ACT & ASSERT
        BadCredentialsException thrown = assertThrows(BadCredentialsException.class, () -> {
            refreshTokenService.validateRefreshToken(MOCK_REFRESH_TOKEN);
        });

        assertTrue(thrown.getErrorCode() == ErrorCode.INVALID_JWT);
    }

    @Test
    void revokeRefreshToken_ShouldThrowBadCredentialsException_WhenClaimsAreEmpty() {
        // ARRANGE
        when(jwtUtils.getAllClaimsFromToken(MOCK_REFRESH_TOKEN)).thenReturn(mockJws);
        when(mockJws.getPayload()).thenReturn(mockClaims);
        when(mockClaims.isEmpty()).thenReturn(true);

        // ACT & ASSERT
        BadCredentialsException thrown = assertThrows(BadCredentialsException.class, () -> {
            refreshTokenService.revokeRefreshToken(MOCK_REFRESH_TOKEN);
        });

        assertTrue(thrown.getErrorCode() == ErrorCode.INVALID_JWT);
    }

    @Test
    void revokeRefreshToken_ShouldThrowBadCredentialsException_WhenJTIIsNull() {
        // ARRANGE
        when(jwtUtils.getAllClaimsFromToken(MOCK_REFRESH_TOKEN)).thenReturn(mockJws);
        when(mockJws.getPayload()).thenReturn(mockClaims);
        when(mockClaims.getId()).thenReturn(null);

        // ACT & ASSERT
        BadCredentialsException thrown = assertThrows(BadCredentialsException.class, () -> {
            refreshTokenService.revokeRefreshToken(MOCK_REFRESH_TOKEN);
        });

        assertTrue(thrown.getErrorCode() == ErrorCode.INVALID_JWT);
    }

    @Test
    void revokeRefreshToken_ShouldThrowBadCredentialsException_WhenJTIIsEmpty() {
        // ARRANGE
        when(jwtUtils.getAllClaimsFromToken(MOCK_REFRESH_TOKEN)).thenReturn(mockJws);
        when(mockJws.getPayload()).thenReturn(mockClaims);
        when(mockClaims.getId()).thenReturn("");

        // ACT & ASSERT
        BadCredentialsException thrown = assertThrows(BadCredentialsException.class, () -> {
            refreshTokenService.revokeRefreshToken(MOCK_REFRESH_TOKEN);
        });

        assertTrue(thrown.getErrorCode() == ErrorCode.INVALID_JWT);
    }

    @Test
    void revokeRefreshToken_ShouldThrowBadCredentialsException_WhenJTIIsInvalidUUID() {
        // ARRANGE
        when(jwtUtils.getAllClaimsFromToken(MOCK_REFRESH_TOKEN)).thenReturn(mockJws);
        when(mockJws.getPayload()).thenReturn(mockClaims);
        when(mockClaims.getId()).thenReturn("invalid-uuid");

        // ACT & ASSERT
        BadCredentialsException thrown = assertThrows(BadCredentialsException.class, () -> {
            refreshTokenService.revokeRefreshToken(MOCK_REFRESH_TOKEN);
        });

        assertTrue(thrown.getErrorCode() == ErrorCode.INVALID_JWT);
    }

    @Test
    void revokeRefreshToken_ShouldCallDeleteById_WhenJTIIsValidUUID() {
        // ARRANGE
        when(jwtUtils.getAllClaimsFromToken(MOCK_REFRESH_TOKEN)).thenReturn(mockJws);
        when(mockJws.getPayload()).thenReturn(mockClaims);
        when(mockClaims.getId()).thenReturn("00000000-0000-0000-0000-000000000000");

        // ACT
        refreshTokenService.revokeRefreshToken(MOCK_REFRESH_TOKEN);

        // ASSERT
        verify(refreshTokenRepository).deleteById(uuid);
    }
}
