package com.sepisac.backend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SecurityException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtTokenProvider {

    private final SecretKey key;
    private final long jwtExpirationMs;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secretKey,
            @Value("${jwt.expiration}") long jwtExpirationMs) {
        this.key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
        this.jwtExpirationMs = jwtExpirationMs;
    }

    public String generateToken(UserPrincipal userPrincipal) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationMs);

        var builder = Jwts.builder()
                .subject(userPrincipal.getId().toString())
                .claim("email", userPrincipal.getEmail())
                .claim("role", userPrincipal.getRole())
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key);

        if (userPrincipal.getCompanyId() != null) {
            builder.claim("company_id", userPrincipal.getCompanyId().toString());
        }

        return builder.compact();
    }

    public boolean validateToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            return false;
        }
        try {
            Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (SecurityException | MalformedJwtException | ExpiredJwtException | UnsupportedJwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String getEmailFromToken(String token) {
        return getClaims(token).get("email", String.class);
    }

    public UUID getCompanyIdFromToken(String token) {
        String companyIdStr = getClaims(token).get("company_id", String.class);
        return (companyIdStr != null && !companyIdStr.trim().isEmpty())
                ? UUID.fromString(companyIdStr)
                : null;
    }

    public String getRoleFromToken(String token) {
        return getClaims(token).get("role", String.class);
    }

    public UUID getUserIdFromToken(String token) {
        String subject = getClaims(token).getSubject();
        return (subject != null && !subject.trim().isEmpty())
                ? UUID.fromString(subject)
                : null;
    }
}
