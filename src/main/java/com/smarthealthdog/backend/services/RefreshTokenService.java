package com.smarthealthdog.backend.services;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.smarthealthdog.backend.domain.RefreshToken;
import com.smarthealthdog.backend.domain.User;
import com.smarthealthdog.backend.exceptions.BadCredentialsException;
import com.smarthealthdog.backend.repositories.RefreshTokenRepository;
import com.smarthealthdog.backend.utils.JWTUtils;
import com.smarthealthdog.backend.validation.ErrorCode;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;

@Service
public class RefreshTokenService {
    private final RefreshTokenRepository refreshTokenRepository; 
    private final JWTUtils jwtUtils;

    // 리프레시 토큰 만료 기간(일)
    @Value("${jwt.refresh-token.expiration.days}")
    private Long refreshTokenExpirationInDays;

    // 유저 당 최대 리프레시 토큰 개수
    @Value("${jwt.refresh-token.max-count}")
    private Integer maxRefreshTokenCount;

    @Autowired
    public RefreshTokenService(
        RefreshTokenRepository refreshTokenRepository,
        JWTUtils jwtUtils
    ) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtUtils = jwtUtils;
    }

    /**
     * 유저의 모든 리프레시 토큰 삭제
     * @param user
     */
    @Transactional
    public void deleteUserRefreshTokens(User user) {
        refreshTokenRepository.deleteByUser(user);
    }

    /**
     * 유저의 만료된 리프레시 토큰 삭제
     * @param user
     */
    @Transactional
    public void deleteUserRefreshTokensIfExpired(User user) {
        Instant now = Instant.now();
        refreshTokenRepository.deleteAllExpiredSinceByUser(now, user);
    }

    /**
     * 특정 리프레시 토큰 삭제
     * @param tokenId UUID 형식의 토큰 ID
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void deleteRefreshTokensById(UUID tokenId) {
        refreshTokenRepository.deleteById(tokenId);
    }

    /**
     * 유저의 리프레시 토큰 개수가 최대 개수를 초과하는지 확인하고, 초과하는 경우 오래된 토큰부터 삭제
     * @param user
     */
    @Transactional
    public void enforceMaxRefreshTokenCount(User user) {
        if (maxRefreshTokenCount == null || maxRefreshTokenCount <= 0) {
            return;
        }

        // 유저의 모든 리프레시 토큰 조회
        List<RefreshToken> tokens = refreshTokenRepository.findByUser(user);
        int tokenCount = tokens.size();

        // 최대 개수를 초과하는 경우 오래된 토큰부터 삭제
        if (tokenCount > maxRefreshTokenCount) {
            int lastTokenToDeleteIndex = tokenCount - maxRefreshTokenCount - 1;
            RefreshToken tokenToDelete = tokens.get(lastTokenToDeleteIndex);

            refreshTokenRepository.deleteAllExpiredSinceByUser(tokenToDelete.getExpiresAt(), user);
        }
    }

    /**
     * 엑세스 토큰 생성
     * @param refreshToken
     * @return 생성된 엑세스 토큰
     * @throws BadCredentialsException 리프레시 토큰이 유효하지 않을 경우 발생
     */
    public String generateAccessToken(String refreshToken) {
        validateRefreshToken(refreshToken);

        Jws<Claims> claims = jwtUtils.getAllClaimsFromToken(refreshToken);
        String userId = claims.getPayload().getSubject();

        Date now = new Date();
        return jwtUtils.generateAccessToken(userId, now);
    }

    /**
     * 리프레시 토큰 생성
     * @param user
     * @return 생성된 리프레시 토큰
     */
    public String generateRefreshToken(User user) {
        if (user == null || user.getId() == null) {
            throw new IllegalArgumentException("User or User ID cannot be null");
        }

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

    /**
     * 토큰에서 클레임 추출
     * @param token
     * @return 토큰의 클레임
     * @throws BadCredentialsException 토큰이 유효하지 않을 경우 발생
     */
    public Jws<Claims> getClaimsFromToken(String token) {
        try {
            jwtUtils.validateJwtToken(token);
            return jwtUtils.getAllClaimsFromToken(token);
        } catch (Exception e) {
            throw new BadCredentialsException(ErrorCode.INVALID_JWT);
        }
    }

    /** 
     * 만료된 토큰 제거
     * @throws RuntimeException DB 오류 발생 시 발생
    */
    @Transactional
    public void removeExpiredTokens() {
        Instant now = Instant.now();
        refreshTokenRepository.deleteAllExpiredSince(now);
    }

    /**
     * 리프레시 토큰 폐기
     * @param token
     * @throws BadCredentialsException 토큰이 유효하지 않을 경우 발생
     */
    public void revokeRefreshToken(String token) {
        if (token == null || token.isEmpty()) {
            throw new BadCredentialsException(ErrorCode.INVALID_JWT);
        }

        // 토큰에서 클레임 추출
        Jws<Claims> claims;
        try {
            claims = jwtUtils.getAllClaimsFromToken(token);
        } catch (Exception e) {
            throw new BadCredentialsException(ErrorCode.INVALID_JWT);
        }

        if (claims == null) {
            throw new BadCredentialsException(ErrorCode.INVALID_JWT);
        }

        // 토큰에서 토큰 ID(jti) 추출
        String tokenId = claims.getPayload().getId();
        if (tokenId == null || tokenId.isEmpty()) {
            throw new BadCredentialsException(ErrorCode.INVALID_JWT);
        }

        // 토큰 ID가 UUID 형식인지 확인
        UUID uuid;
        try {
            uuid = UUID.fromString(tokenId);
        } catch (IllegalArgumentException e) {
            throw new BadCredentialsException(ErrorCode.INVALID_JWT);
        }

        // 토큰 ID가 데이터베이스에서 삭제
        refreshTokenRepository.deleteById(uuid);
    }

    /**
     * 엑세스 토큰 유효성 검사
     * @param token
     * @throws BadCredentialsException 엑세스 토큰이 유효하지 않을 경우 발생
     */
    public void validateAccessToken(String token) throws BadCredentialsException {
        if (token == null || token.isEmpty()) {
            throw new BadCredentialsException(ErrorCode.INVALID_JWT);
        }

        // 토큰에서 클레임 추출
        Jws<Claims> claims;
        try {
            claims = jwtUtils.getAllClaimsFromToken(token);
        } catch (Exception e) {
            throw new BadCredentialsException(ErrorCode.INVALID_JWT);
        }

        if (claims == null) {
            throw new BadCredentialsException(ErrorCode.INVALID_JWT);
        }

        // 토큰에서 사용자 ID(sub) 추출
        String userId = claims.getPayload().getSubject();
        if (userId == null || userId.isEmpty()) {
            throw new BadCredentialsException(ErrorCode.INVALID_JWT);
        }

        // 유저 ID가 숫자 형식인지 확인
        try {
            Long.parseLong(userId);
        } catch (NumberFormatException e) {
            throw new BadCredentialsException(ErrorCode.INVALID_JWT);
        }

        // 토큰 만료 시간(exp) 확인
        Date expiration = claims.getPayload().getExpiration();
        if (expiration == null || expiration.before(new Date())) {
            throw new BadCredentialsException(ErrorCode.INVALID_JWT);
        }
    }

    /**
     * 리프레시 토큰 유효성 검사
     * @param token
     * @throws BadCredentialsException 리프레시 토큰이 유효하지 않을 경우 발생
     */
    public void validateRefreshToken(String token) throws BadCredentialsException {
        if (token == null || token.isEmpty()) {
            throw new BadCredentialsException(ErrorCode.INVALID_JWT);
        }

        // 토큰에서 클레임 추출
        Jws<Claims> claims;
        try {
            claims = jwtUtils.getAllClaimsFromToken(token);
        } catch (Exception e) {
            throw new BadCredentialsException(ErrorCode.INVALID_JWT);
        }

        if (claims == null) {
            throw new BadCredentialsException(ErrorCode.INVALID_JWT);
        }

        // 토큰에서 유저 ID(sub) 추출
        String userId = claims.getPayload().getSubject();
        if (userId == null || userId.isEmpty()) {
            throw new BadCredentialsException(ErrorCode.INVALID_JWT);
        }

        // 유저 ID가 숫자 형식인지 확인
        try {
            Long.parseLong(userId);
        } catch (NumberFormatException e) {
            throw new BadCredentialsException(ErrorCode.INVALID_JWT);
        }

        // 토큰에서 토큰 ID(jti) 추출
        String tokenId = claims.getPayload().getId();
        if (tokenId == null || tokenId.isEmpty()) {
            throw new BadCredentialsException(ErrorCode.INVALID_JWT);
        }

        // 토큰 ID가 UUID 형식인지 확인
        UUID tokenUuid;
        try {
            tokenUuid = UUID.fromString(tokenId);
        } catch (IllegalArgumentException e) {
            throw new BadCredentialsException(ErrorCode.INVALID_JWT);
        }

        // 토큰 ID가 데이터베이스에 존재하는지 확인
        boolean storedToken = refreshTokenRepository.existsById(tokenUuid);
        if (storedToken == false) {
            throw new BadCredentialsException(ErrorCode.INVALID_JWT);
        }

        // 토큰 만료 시간(exp) 확인
        Date expiration = claims.getPayload().getExpiration();
        if (expiration == null || expiration.before(new Date())) {
            deleteRefreshTokensById(tokenUuid);
            throw new BadCredentialsException(ErrorCode.INVALID_JWT);
        }
    }
}