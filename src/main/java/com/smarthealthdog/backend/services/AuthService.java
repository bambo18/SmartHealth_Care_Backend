package com.smarthealthdog.backend.services;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smarthealthdog.backend.domain.RoleEnum;
import com.smarthealthdog.backend.domain.User;
import com.smarthealthdog.backend.dto.LoginResponse;
import com.smarthealthdog.backend.dto.UserCreateRequest;
import com.smarthealthdog.backend.exceptions.BadCredentialsException;
import com.smarthealthdog.backend.exceptions.InvalidRequestDataException;
import com.smarthealthdog.backend.exceptions.ResourceNotFoundException;
import com.smarthealthdog.backend.validation.ErrorCode;

@Service
public class AuthService {
    private final RefreshTokenService refreshTokenService;
    private final RefreshTokenCleanupService refreshTokenCleanupService;
    private final UserService userService;

    @Autowired
    public AuthService(
        RefreshTokenService refreshTokenService,
        RefreshTokenCleanupService refreshTokenCleanupService,
        UserService userService
    ) {
        this.refreshTokenService = refreshTokenService;
        this.refreshTokenCleanupService = refreshTokenCleanupService;
        this.userService = userService;
    }

    /**
     * 유저 활성화(이메일 인증)
     * @param user
     */
    @Transactional
    public void activateUser(User user) {
        // 역할이 UNVERIFIED_USER이 아닌 경우(이미 인증된 경우) 예외 발생
        if (!user.getRole().getName().equals(RoleEnum.UNVERIFIED_USER)) {
            throw new InvalidRequestDataException(ErrorCode.INVALID_EMAIL_VERIFICATION);
        }

        userService.changeRoleToVerifiedUser(user);
        userService.expireEmailVerificationToken(user);
        userService.resetEmailVerificationFailCount(user);
    }


    /**
     * 로그인 시 액세스 토큰과 리프레시 토큰 생성
     * @param userId
     * @return
     */
    @Transactional
    public LoginResponse generateTokens(Long userId) {
        User user = userService.getUserById(userId)
            .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.RESOURCE_NOT_FOUND));

        // 만료된 리프레시 토큰 삭제
        refreshTokenCleanupService.deleteUserRefreshTokensIfExpired(user);

        String refreshToken = refreshTokenService.generateRefreshToken(user);
        String accessToken = refreshTokenService.generateAccessToken(refreshToken);
        String accessExpiration = refreshTokenService.getExpirationFromTokenInISOString(accessToken);

        // 유저의 리프레시 토큰 개수가 최대 개수를 초과하는지 확인하고, 초과하는 경우 오래된 토큰부터 삭제
        refreshTokenCleanupService.enforceMaxRefreshTokenCount(user);

        return new LoginResponse(accessToken, refreshToken, accessExpiration);
    }

    /**
     * 로그아웃 시 리프레시 토큰 무효화
     * @param refreshToken
     * @return
     */
    @Transactional
    public void invalidateRefreshToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isEmpty()) {
            throw new BadCredentialsException(ErrorCode.INVALID_JWT);
        }

        // 리프레시 토큰이 유효한지 확인
        refreshTokenService.validateRefreshToken(refreshToken);

        User user = refreshTokenService.getUserFromToken(refreshToken);
        if (user == null) {
            throw new BadCredentialsException(ErrorCode.INVALID_JWT);
        }

        // 만료된 리프레시 토큰 삭제
        refreshTokenCleanupService.deleteUserRefreshTokensIfExpired(user);

        // 해당 리프레시 토큰 삭제
        UUID tokenId = refreshTokenService.getTokenIdFromToken(refreshToken);
        refreshTokenCleanupService.deleteRefreshTokensById(tokenId);
    }

    /**
     * 리프레시 토큰을 사용하여 새로운 액세스 토큰과 리프레시 토큰 생성
     * @param refreshToken
     * @return
     * @throws BadCredentialsException 토큰이 유효하지 않을 경우 발생
     */
    @Transactional
    public LoginResponse refreshAccessToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isEmpty()) {
            throw new BadCredentialsException(ErrorCode.LOGIN_FAILURE);
        }

        // 리프레시 토큰이 유효한지 확인
        refreshTokenService.validateRefreshToken(refreshToken);

        User user = refreshTokenService.getUserFromToken(refreshToken);
        if (user == null) {
            throw new BadCredentialsException(ErrorCode.LOGIN_FAILURE);
        }

        // 만료된 리프레시 토큰 삭제
        refreshTokenCleanupService.deleteUserRefreshTokensIfExpired(user);

        String newRefreshToken = refreshTokenService.rotateRefreshToken(refreshToken);
        String accessToken = refreshTokenService.generateAccessToken(newRefreshToken);
        String accessExpiration = refreshTokenService.getExpirationFromTokenInISOString(accessToken);

        refreshTokenCleanupService.enforceMaxRefreshTokenCount(user);

        return new LoginResponse(accessToken, newRefreshToken, accessExpiration);
    }

    /**
     * 유저 회원가입
     * @param request
     * @return 생성된 유저 객체
     */
    public User registerUser(UserCreateRequest request) {
        return userService.createUser(
            request.nickname(),
            request.email(),
            request.password()
        );
    }

    /**
     * 이메일 인증 토큰 검증
     * @param email
     * @param token
     * @return 검증된 유저 객체
     * @throws InvalidRequestDataException 토큰이 유효하지 않을 경우 발생
     */
    public User verifyEmailToken(String email, String token) {
        if (email == null || token == null) {
            throw new InvalidRequestDataException(ErrorCode.INVALID_EMAIL_VERIFICATION);
        }

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
}
