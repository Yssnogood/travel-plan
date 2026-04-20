package com.travelplan.shared.security;

import com.travelplan.shared.dto.UserContext;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * JWT utility class for token generation and validation
 */
@Slf4j
@Component
public class JwtUtils {

    private final SecretKey secretKey;
    private final long accessTokenExpiration;
    private final long refreshTokenExpiration;

    public JwtUtils(
            @Value("${jwt.secret:your-super-secret-jwt-key-that-should-be-at-least-256-bits-long}") String secret,
            @Value("${jwt.access-token-expiration:3600000}") long accessTokenExpiration,
            @Value("${jwt.refresh-token-expiration:604800000}") long refreshTokenExpiration) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpiration = accessTokenExpiration;
        this.refreshTokenExpiration = refreshTokenExpiration;
    }

    public String generateAccessToken(UserContext userContext) {
        return generateToken(userContext, accessTokenExpiration);
    }

    public String generateRefreshToken(UserContext userContext) {
        return generateToken(userContext, refreshTokenExpiration);
    }

    private String generateToken(UserContext userContext, long expiration) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userContext.getUserId());
        claims.put("email", userContext.getEmail());
        claims.put("firstName", userContext.getFirstName());
        claims.put("lastName", userContext.getLastName());
        claims.put("role", userContext.getRole());
        claims.put("permissions", userContext.getPermissions());

        return Jwts.builder()
                .claims(claims)
                .subject(userContext.getEmail())
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(secretKey)
                .compact();
    }

    public UserContext validateTokenAndGetUser(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            return UserContext.builder()
                    .userId(claims.get("userId", Long.class))
                    .email(claims.get("email", String.class))
                    .firstName(claims.get("firstName", String.class))
                    .lastName(claims.get("lastName", String.class))
                    .role(claims.get("role", String.class))
                    .permissions(getPermissionsFromClaims(claims))
                    .build();
        } catch (JwtException ex) {
            log.warn("Invalid JWT token: {}", ex.getMessage());
            return null;
        }
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException ex) {
            log.warn("Invalid JWT token: {}", ex.getMessage());
            return false;
        }
    }

    public String getEmailFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claims.getSubject();
    }

    public Date getExpirationFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claims.getExpiration();
    }

    @SuppressWarnings("unchecked")
    private List<String> getPermissionsFromClaims(Claims claims) {
        Object permissions = claims.get("permissions");
        if (permissions instanceof List) {
            return (List<String>) permissions;
        }
        return Collections.emptyList();
    }
}
