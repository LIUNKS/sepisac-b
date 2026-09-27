package com.sepisac.backend.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class AuthCookieProvider {

    private final String cookieName;
    private final boolean cookieSecure;
    private final String cookieSameSite;
    private final String cookiePath;
    private final long jwtExpirationMs;

    public AuthCookieProvider(
            @Value("${jwt.cookie.name}") String cookieName,
            @Value("${jwt.cookie.secure}") boolean cookieSecure,
            @Value("${jwt.cookie.same-site}") String cookieSameSite,
            @Value("${jwt.cookie.path}") String cookiePath,
            @Value("${jwt.expiration}") long jwtExpirationMs) {
        this.cookieName = cookieName;
        this.cookieSecure = cookieSecure;
        this.cookieSameSite = cookieSameSite;
        this.cookiePath = cookiePath;
        this.jwtExpirationMs = jwtExpirationMs;
    }

    public ResponseCookie createAuthCookie(String token) {
        return ResponseCookie.from(cookieName, token)
                .httpOnly(true)
                .secure(cookieSecure)
                .path(cookiePath)
                .maxAge(jwtExpirationMs / 1000)
                .sameSite(cookieSameSite)
                .build();
    }

    public ResponseCookie createCleanAuthCookie() {
        return ResponseCookie.from(cookieName, "")
                .httpOnly(true)
                .secure(cookieSecure)
                .path(cookiePath)
                .maxAge(0)
                .sameSite(cookieSameSite)
                .build();
    }

    public String extractToken(HttpServletRequest request) {
        if (request == null || request.getCookies() == null) {
            return null;
        }

        for (Cookie cookie : request.getCookies()) {
            if (cookieName.equals(cookie.getName())) {
                String value = cookie.getValue();
                if (StringUtils.hasText(value)) {
                    return value.trim();
                }
            }
        }

        return null;
    }

    public String getCookieName() {
        return cookieName;
    }
}
