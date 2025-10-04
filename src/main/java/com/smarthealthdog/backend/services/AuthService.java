package com.smarthealthdog.backend.services;

import java.time.Instant;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smarthealthdog.backend.domain.RoleEnum;
import com.smarthealthdog.backend.domain.User;
import com.smarthealthdog.backend.dto.LoginResponse;
import com.smarthealthdog.backend.dto.UserCreateRequest;
import com.smarthealthdog.backend.exceptions.InvalidRequestDataException;
import com.smarthealthdog.backend.exceptions.ResourceNotFoundException;
import com.smarthealthdog.backend.validation.ErrorCode;

@Service
public class AuthService {
    private final RefreshTokenService refreshTokenService;
    private final UserService userService;

    @Autowired
    public AuthService(
        RefreshTokenService refreshTokenService,
        UserService userService
    ) {
        this.refreshTokenService = refreshTokenService;
        this.userService = userService;
    }

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

    // Generate both access and refresh tokens
    @Transactional
    public LoginResponse generateTokens(Long userId) {
        User user = userService.getUserById(userId)
            .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.RESOURCE_NOT_FOUND));

        String refreshToken = refreshTokenService.generateRefreshToken(user);
        String accessToken = refreshTokenService.generateAccessToken(refreshToken);

        return new LoginResponse(accessToken, refreshToken);
    }

    public User registerUser(UserCreateRequest request) {
        return userService.createUser(
            request.nickname(),
            request.email(),
            request.password()
        );
    }

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

}
