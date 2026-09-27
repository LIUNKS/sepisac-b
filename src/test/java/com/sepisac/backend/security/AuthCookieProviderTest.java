package com.sepisac.backend.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseCookie;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("AuthCookieProvider Unit Tests")
class AuthCookieProviderTest {

    private AuthCookieProvider authCookieProvider;

    private static final String COOKIE_NAME = "jwt_token";
    private static final boolean COOKIE_SECURE = false;
    private static final String COOKIE_SAME_SITE = "Lax";
    private static final String COOKIE_PATH = "/";
    private static final long JWT_EXPIRATION_MS = 86400000L; // 24 hours (86400 seconds)

    @BeforeEach
    void setUp() {
        authCookieProvider = new AuthCookieProvider(
                COOKIE_NAME,
                COOKIE_SECURE,
                COOKIE_SAME_SITE,
                COOKIE_PATH,
                JWT_EXPIRATION_MS
        );
    }

    @Nested
    @DisplayName("createAuthCookie")
    class CreateAuthCookieTests {

        @Test
        @DisplayName("Should create HttpOnly cookie with valid token and expiration")
        void shouldCreateValidAuthCookie() {
            String token = "sample.jwt.token";

            ResponseCookie cookie = authCookieProvider.createAuthCookie(token);

            assertThat(cookie.getName()).isEqualTo(COOKIE_NAME);
            assertThat(cookie.getValue()).isEqualTo(token);
            assertThat(cookie.isHttpOnly()).isTrue();
            assertThat(cookie.isSecure()).isEqualTo(COOKIE_SECURE);
            assertThat(cookie.getPath()).isEqualTo(COOKIE_PATH);
            assertThat(cookie.getMaxAge().getSeconds()).isEqualTo(86400L);
            assertThat(cookie.getSameSite()).isEqualTo("Lax");
        }
    }

    @Nested
    @DisplayName("createCleanAuthCookie")
    class CreateCleanAuthCookieTests {

        @Test
        @DisplayName("Should create clean cookie with maxAge 0 to clear cookie on logout")
        void shouldCreateCleanCookieWithMaxAgeZero() {
            ResponseCookie cookie = authCookieProvider.createCleanAuthCookie();

            assertThat(cookie.getName()).isEqualTo(COOKIE_NAME);
            assertThat(cookie.getValue()).isEmpty();
            assertThat(cookie.isHttpOnly()).isTrue();
            assertThat(cookie.isSecure()).isEqualTo(COOKIE_SECURE);
            assertThat(cookie.getPath()).isEqualTo(COOKIE_PATH);
            assertThat(cookie.getMaxAge().getSeconds()).isZero();
            assertThat(cookie.getSameSite()).isEqualTo("Lax");
        }
    }

    @Nested
    @DisplayName("extractToken")
    class ExtractTokenTests {

        @Test
        @DisplayName("Should extract token from request cookies when present")
        void shouldExtractTokenWhenCookieIsPresent() {
            HttpServletRequest request = mock(HttpServletRequest.class);
            Cookie[] cookies = new Cookie[]{
                    new Cookie("other_cookie", "some_value"),
                    new Cookie(COOKIE_NAME, "extracted.jwt.token")
            };
            when(request.getCookies()).thenReturn(cookies);

            String token = authCookieProvider.extractToken(request);

            assertThat(token).isEqualTo("extracted.jwt.token");
        }

        @Test
        @DisplayName("Should return null when cookies array is null")
        void shouldReturnNullWhenCookiesArrayIsNull() {
            HttpServletRequest request = mock(HttpServletRequest.class);
            when(request.getCookies()).thenReturn(null);

            String token = authCookieProvider.extractToken(request);

            assertThat(token).isNull();
        }

        @Test
        @DisplayName("Should return null when cookie name is not found")
        void shouldReturnNullWhenCookieNotFound() {
            HttpServletRequest request = mock(HttpServletRequest.class);
            Cookie[] cookies = new Cookie[]{
                    new Cookie("other_cookie", "some_value")
            };
            when(request.getCookies()).thenReturn(cookies);

            String token = authCookieProvider.extractToken(request);

            assertThat(token).isNull();
        }

        @Test
        @DisplayName("Should return null when cookie value is blank")
        void shouldReturnNullWhenCookieValueIsBlank() {
            HttpServletRequest request = mock(HttpServletRequest.class);
            Cookie[] cookies = new Cookie[]{
                    new Cookie(COOKIE_NAME, "   ")
            };
            when(request.getCookies()).thenReturn(cookies);

            String token = authCookieProvider.extractToken(request);

            assertThat(token).isNull();
        }
    }
}
