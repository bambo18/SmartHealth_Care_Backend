package com.smarthealthdog.backend.services;

import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smarthealthdog.backend.domain.RefreshToken;
import com.smarthealthdog.backend.domain.RoleEnum;
import com.smarthealthdog.backend.domain.User;
import com.smarthealthdog.backend.dto.LoginResponse;
import com.smarthealthdog.backend.dto.UserCreateRequest;
import com.smarthealthdog.backend.exceptions.BadCredentialsException;
import com.smarthealthdog.backend.exceptions.InvalidRequestDataException;
import com.smarthealthdog.backend.exceptions.ResourceNotFoundException;
import com.smarthealthdog.backend.repositories.RefreshTokenRepository;
import com.smarthealthdog.backend.utils.JWTUtils;
import com.smarthealthdog.backend.validation.ErrorCode;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;

@Service
public class AuthService {
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserService userService;
    private final JWTUtils jwtUtils;

    @Value("${jwt.refresh-token.expiration.days}")
    private Long refreshTokenExpirationInDays;

    @Autowired
    public AuthService(
        RefreshTokenRepository refreshTokenRepository, 
        UserService userService,
        JWTUtils jwtUtils
    ) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.userService = userService;
        this.jwtUtils = jwtUtils;
    }

    public User registerUser(UserCreateRequest request) {
        return userService.createUser(
            request.nickname(),
            request.email(),
            request.password()
        );
    }

    @Transactional
    public User verifyEmailToken(String email, String token) {
        Optional<User> userOpt = userService.getUserByEmail(email);
        // 이메일로 사용자를 찾을 수 없는 경우 예외 발생
        if (userOpt.isEmpty()) {
            throw new InvalidRequestDataException(ErrorCode.INVALID_EMAIL_VERIFICATION);
        }

        User user = userOpt.get();

        if (user.getEmailVerificationFailCount() >= 5) {
            throw new InvalidRequestDataException(ErrorCode.INVALID_EMAIL_VERIFICATION);
        }

        // 역할이 UNVERIFIED_USER이 아닌 경우(이미 인증된 경우) 예외 발생
        if (!user.getRole().getName().equals(RoleEnum.UNVERIFIED_USER)) {
            throw new InvalidRequestDataException(ErrorCode.INVALID_EMAIL_VERIFICATION);
        }

        // 토큰 인증 시간이 만료되었는지 확인
        Instant expiry = user.getEmailVerificationExpiry();
        if (expiry == null || Instant.now().isAfter(expiry)) {
            throw new InvalidRequestDataException(ErrorCode.INVALID_EMAIL_VERIFICATION);
        }

        // 토큰이 불일치하는 경우 예외 발생
        if (!token.equals(user.getEmailVerificationToken())) {
            userService.incrementEmailVerificationFailCount(user);
            throw new InvalidRequestDataException(ErrorCode.INVALID_EMAIL_VERIFICATION);
        }

        return user;
    }

    @Transactional
    public void activateUser(User user) {
        // 역할이 UNVERIFIED_USER이 아닌 경우(이미 인증된 경우) 예외 발생
        if (!user.getRole().getName().equals(RoleEnum.UNVERIFIED_USER)) {
            throw new InvalidRequestDataException(ErrorCode.INVALID_EMAIL_VERIFICATION);
        }

        userService.changeUnverifiedUserToUser(user);
        userService.expireEmailVerificationToken(user);
    }

    public String generateAccessToken(String refreshToken) {
        if (validateRefreshToken(refreshToken) == false) {
            throw new BadCredentialsException(ErrorCode.INVALID_JWT);
        }

        Jws<Claims> claims = jwtUtils.getAllClaimsFromToken(refreshToken);
        if (claims == null) {
            throw new BadCredentialsException(ErrorCode.INVALID_JWT);
        }

        Date expiration = claims.getPayload().getExpiration();
        if (expiration == null || expiration.before(new Date())) {
            throw new BadCredentialsException(ErrorCode.INVALID_JWT);
        }

        String userId = claims.getPayload().getSubject();

        Date now = new Date();
        return jwtUtils.generateAccessToken(userId, now);
    }

    // Generate both access and refresh tokens
    public LoginResponse generateTokens(Long userId) {
        String refreshToken = generateRefreshToken(userId);
        String accessToken = generateAccessToken(refreshToken);

        return new LoginResponse(accessToken, refreshToken);
    }

    // Add methods to manage refresh tokens as needed
    public String generateRefreshToken(Long userId) {
        Optional<User> userOpt = userService.getUserById(userId);
        if (userOpt.isEmpty()) {
            throw new ResourceNotFoundException(ErrorCode.RESOURCE_NOT_FOUND);
        }

        User user = userOpt.get();

        Date issuedAt = new Date();
        Instant expiresAt = Instant.now().plusSeconds(refreshTokenExpirationInDays * 24 * 60 * 60);

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setId(UUID.randomUUID());
        refreshToken.setExpiresAt(expiresAt);

        refreshTokenRepository.save(refreshToken);
        return jwtUtils.generateRefreshToken(
            user.getId().toString(), 
            refreshToken.getId(), 
            issuedAt
        );
    }

    public boolean validateAccessToken(String token) {
        // Implementation for validating an access token
        Jws<Claims> claims = jwtUtils.getAllClaimsFromToken(token);
        if (claims == null) {
            return false;
        }

        // Check if the token is expired
        Date expiration = claims.getPayload().getExpiration();
        if (expiration == null || expiration.before(new Date())) {
            return false;
        }

        return true;
    }

    public boolean validateRefreshToken(String token) {
        // Implementation for validating a refresh token
        Jws<Claims> claims = jwtUtils.getAllClaimsFromToken(token);
        if (claims == null) {
            return false;
        }

        // Extract the token ID (jti) from the claims
        String tokenId = claims.getPayload().getId();
        if (tokenId == null || tokenId.isEmpty()) {
            return false;
        }

        UUID tokenUuid;
        try {
            tokenUuid = UUID.fromString(tokenId);
        } catch (IllegalArgumentException e) {
            return false;
        }

        // Check if the token exists in the database
        boolean storedToken = refreshTokenRepository.existsById(tokenUuid);
        if (storedToken == false) {
            return false;
        }

        // Check if the token is expired
        Date expiration = claims.getPayload().getExpiration();
        if (expiration == null || expiration.before(new Date())) {
            return false;
        }

        return true;
    }

    public Jws<Claims> getClaimsFromToken(String token) {
        return jwtUtils.getAllClaimsFromToken(token);
    }

    public void revokeRefreshToken(String token) {
        Jws<Claims> claims = jwtUtils.getAllClaimsFromToken(token);
        if (claims == null) {
            throw new BadCredentialsException(ErrorCode.INVALID_JWT);
        }

        String tokenId = claims.getPayload().getId();
        if (tokenId == null || tokenId.isEmpty()) {
            throw new BadCredentialsException(ErrorCode.INVALID_JWT);
        }

        UUID uuid;
        try {
            uuid = UUID.fromString(tokenId);
        } catch (IllegalArgumentException e) {
            throw new BadCredentialsException(ErrorCode.INVALID_JWT);
        }

        refreshTokenRepository.deleteById(uuid);
    }
}
