package org.zemo.omninet.security.service;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.zemo.omninet.security.model.User;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class JwtService {

    @Value("${jwt.secret}")
    private String secretKey;

    @Getter
    @Value("${jwt.access-token-expiration}")
    private long accessTokenExpiration;

    @Getter
    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    @Value("${jwt.issuer}")
    private String issuer;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secretKey.getBytes());
    }

    public String generateAccessToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("email", user.getEmail());
        claims.put("name", user.getName());
        claims.put("tokenType", "access");

        return Jwts.builder()
                .claims(claims)
                .subject(user.getId())
                .issuer(issuer)
                .issuedAt(new Date())
                .expiration(Date.from(Instant.now().plus(accessTokenExpiration, ChronoUnit.MILLIS)))
                .signWith(getSigningKey())
                .compact();
    }

    public String generateRefreshToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("email", user.getEmail());
        claims.put("tokenType", "refresh");

        return Jwts.builder()
                .claims(claims)
                .subject(user.getId())
                .issuer(issuer)
                .issuedAt(new Date())
                .expiration(Date.from(Instant.now().plus(refreshTokenExpiration, ChronoUnit.MILLIS)))
                .signWith(getSigningKey())
                .compact();
    }

    public Claims validateToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            log.warn("JWT token is expired: {}", e.getMessage());
            throw new RuntimeException("Token expired", e);
        } catch (UnsupportedJwtException e) {
            log.warn("JWT token is unsupported: {}", e.getMessage());
            throw new RuntimeException("Unsupported token", e);
        } catch (MalformedJwtException e) {
            log.warn("JWT token is malformed: {}", e.getMessage());
            throw new RuntimeException("Malformed token", e);
        } catch (SecurityException e) {
            log.warn("JWT signature validation failed: {}", e.getMessage());
            throw new RuntimeException("Invalid token signature", e);
        } catch (IllegalArgumentException e) {
            log.warn("JWT token compact of handler are invalid: {}", e.getMessage());
            throw new RuntimeException("Invalid token", e);
        }
    }

    public boolean isTokenValid(String token) {
        try {
            Claims claims = validateToken(token);
            return !isTokenExpired(claims);
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isTokenExpired(Claims claims) {
        return claims.getExpiration().before(new Date());
    }

    public String getUserIdFromToken(String token) {
        Claims claims = validateToken(token);
        return claims.getSubject();
    }

    public String getEmailFromToken(String token) {
        Claims claims = validateToken(token);
        return claims.get("email", String.class);
    }

    public String getTokenTypeFromToken(String token) {
        Claims claims = validateToken(token);
        return claims.get("tokenType", String.class);
    }

    public boolean isAccessToken(String token) {
        return "access".equals(getTokenTypeFromToken(token));
    }

    public boolean isRefreshToken(String token) {
        return "refresh".equals(getTokenTypeFromToken(token));
    }

}
