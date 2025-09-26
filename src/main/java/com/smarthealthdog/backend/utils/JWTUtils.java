package com.smarthealthdog.backend.utils;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

@Component
public class JWTUtils {
    @Value("${jwt.secret}")
    private String jwtSecret;
    @Value("${jwt.access-token.expiration.minutes}")
    private int jwtAccessExpirationInMinutes;
    @Value("${jwt.refresh-token.expiration.days}")
    private int jwtRefreshExpirationInDays;
    private SecretKey key;

    // Initializes the key after the class is instantiated and the jwtSecret is injected, 
    // preventing the repeated creation of the key and enhancing performance
    @PostConstruct
    public void init() {
        this.key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    // Generate JWT Access Token
    public String generateAccessToken(String id, Date issuedAt) {
        Date expiration = Date.from(issuedAt.toInstant().plusSeconds(jwtAccessExpirationInMinutes * 60));
        return Jwts.builder()
            .subject(id)
            .issuedAt(issuedAt)
            .expiration(expiration)
            .signWith(key)
            .compact();
    }

    // Generate JWT Refresh Token
    public String generateRefreshToken(String id, UUID uuid, Date issuedAt) {
        Date expiration = Date.from(issuedAt.toInstant().plusSeconds(jwtRefreshExpirationInDays * 24 * 60 * 60));
        return Jwts.builder()
            .subject(id)
            .id(uuid.toString())
            .issuedAt(issuedAt)
            .expiration(expiration)
            .signWith(key)
            .compact();
    }

    // Get user ID from JWT token
    public String getUserIdFromToken(String token) {
        return Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .getPayload()
            .getSubject();
    }

    public Jws<Claims> getAllClaimsFromToken(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
    }

    // Validate JWT token
    public boolean validateJwtToken(String token) {
        try {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
            return true; 
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
