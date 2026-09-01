package com.rentify.auth.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {

    private final SecretKey key;
    private final long expirationMillis;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expires-in:7d}") String expiresIn
    ) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMillis = parseDuration(expiresIn);
    }

    private long parseDuration(String durationStr) {
        if (durationStr == null || durationStr.isBlank()) {
            return 7L * 24 * 60 * 60 * 1000;
        }
        durationStr = durationStr.trim().toLowerCase();
        if (durationStr.endsWith("d")) {
            long days = Long.parseLong(durationStr.substring(0, durationStr.length() - 1));
            return days * 24 * 60 * 60 * 1000;
        } else if (durationStr.endsWith("h")) {
            long hours = Long.parseLong(durationStr.substring(0, durationStr.length() - 1));
            return hours * 60 * 60 * 1000;
        } else if (durationStr.endsWith("m")) {
            long minutes = Long.parseLong(durationStr.substring(0, durationStr.length() - 1));
            return minutes * 60 * 1000;
        } else if (durationStr.endsWith("s")) {
            long seconds = Long.parseLong(durationStr.substring(0, durationStr.length() - 1));
            return seconds * 1000;
        }
        return Long.parseLong(durationStr);
    }

    public String generateToken(Long userId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMillis);

        return Jwts.builder()
                .claim("userId", userId)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key)
                .compact();
    }

    public Long extractUserId(String token) {
        Claims claims = extractAllClaims(token);
        Object userIdObj = claims.get("userId");
        if (userIdObj instanceof Number number) {
            return number.longValue();
        } else if (userIdObj instanceof String str) {
            return Long.parseLong(str);
        }
        throw new JwtException("Missing userId claim in token");
    }

    public boolean validateToken(String token) {
        try {
            extractAllClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
