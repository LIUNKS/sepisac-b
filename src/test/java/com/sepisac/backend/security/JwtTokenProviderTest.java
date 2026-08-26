package com.sepisac.backend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("JwtTokenProvider Security Unit Tests")
class JwtTokenProviderTest {

    private static final String SECRET_KEY_STRING = "claveSecretaSuperSeguraParaDesarrolloLocalDeAlMenos256Bits12345";
    private static final long EXPIRATION_MS = 86400000L;

    private JwtTokenProvider jwtTokenProvider;
    private UserPrincipal testUserPrincipal;
    private UUID userId;
    private UUID companyId;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider(SECRET_KEY_STRING, EXPIRATION_MS);

        userId = UUID.randomUUID();
        companyId = UUID.randomUUID();

        testUserPrincipal = new UserPrincipal(
                userId,
                "admin@sepisac.com",
                "admin_user",
                "encodedPassword123",
                companyId,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN_EMPRESA")),
                true
        );
    }

    @Nested
    @DisplayName("generateToken")
    class GenerateTokenTests {

        @Test
        @DisplayName("Should generate valid JWT with correct subject, email, role, and company_id claims")
        void shouldGenerateTokenWithCorrectClaims() {
            String token = jwtTokenProvider.generateToken(testUserPrincipal);

            assertThat(token).isNotBlank();

            SecretKey key = Keys.hmacShaKeyFor(SECRET_KEY_STRING.getBytes(StandardCharsets.UTF_8));
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            assertThat(claims.getSubject()).isEqualTo(userId.toString());
            assertThat(claims.get("email", String.class)).isEqualTo("admin@sepisac.com");
            assertThat(claims.get("role", String.class)).isEqualTo("ROLE_ADMIN_EMPRESA");
            assertThat(claims.get("company_id", String.class)).isEqualTo(companyId.toString());
            assertThat(claims.getExpiration()).isAfter(new Date());
        }

        @Test
        @DisplayName("Should generate valid JWT when company_id is null for SUPERADMIN")
        void shouldGenerateTokenWithNullCompanyIdForSuperadmin() {
            UserPrincipal superAdmin = new UserPrincipal(
                    userId,
                    "superadmin@sepisac.com",
                    "superadmin",
                    "encodedPassword123",
                    null,
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_SUPERADMIN")),
                    true
            );

            String token = jwtTokenProvider.generateToken(superAdmin);

            assertThat(token).isNotBlank();

            SecretKey key = Keys.hmacShaKeyFor(SECRET_KEY_STRING.getBytes(StandardCharsets.UTF_8));
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            assertThat(claims.getSubject()).isEqualTo(userId.toString());
            assertThat(claims.get("email", String.class)).isEqualTo("superadmin@sepisac.com");
            assertThat(claims.get("role", String.class)).isEqualTo("ROLE_SUPERADMIN");
            assertThat(claims.get("company_id")).isNull();
        }
    }

    @Nested
    @DisplayName("validateToken")
    class ValidateTokenTests {

        @Test
        @DisplayName("Should return true for valid non-expired token")
        void shouldReturnTrueForValidToken() {
            String token = jwtTokenProvider.generateToken(testUserPrincipal);

            boolean isValid = jwtTokenProvider.validateToken(token);

            assertThat(isValid).isTrue();
        }

        @Test
        @DisplayName("Should return false for expired token")
        void shouldReturnFalseForExpiredToken() {
            JwtTokenProvider expiredProvider = new JwtTokenProvider(SECRET_KEY_STRING, -1000L);
            String expiredToken = expiredProvider.generateToken(testUserPrincipal);

            boolean isValid = jwtTokenProvider.validateToken(expiredToken);

            assertThat(isValid).isFalse();
        }

        @Test
        @DisplayName("Should return false for malformed token")
        void shouldReturnFalseForMalformedToken() {
            String malformedToken = "invalid.token.structure";

            boolean isValid = jwtTokenProvider.validateToken(malformedToken);

            assertThat(isValid).isFalse();
        }

        @Test
        @DisplayName("Should return false for token with invalid signature")
        void shouldReturnFalseForInvalidSignature() {
            String otherSecret = "otraClaveSecretaCompletamenteDiferenteParaTesteoDeFirmaInvalida12345";
            JwtTokenProvider otherProvider = new JwtTokenProvider(otherSecret, EXPIRATION_MS);
            String tokenSignedWithDifferentKey = otherProvider.generateToken(testUserPrincipal);

            boolean isValid = jwtTokenProvider.validateToken(tokenSignedWithDifferentKey);

            assertThat(isValid).isFalse();
        }

        @Test
        @DisplayName("Should return false for null or empty token")
        void shouldReturnFalseForNullOrEmptyToken() {
            assertThat(jwtTokenProvider.validateToken(null)).isFalse();
            assertThat(jwtTokenProvider.validateToken("")).isFalse();
            assertThat(jwtTokenProvider.validateToken("   ")).isFalse();
        }
    }

    @Nested
    @DisplayName("Extract Claims")
    class ExtractClaimsTests {

        @Test
        @DisplayName("Should extract email correctly from token")
        void shouldExtractEmail() {
            String token = jwtTokenProvider.generateToken(testUserPrincipal);

            String email = jwtTokenProvider.getEmailFromToken(token);

            assertThat(email).isEqualTo("admin@sepisac.com");
        }

        @Test
        @DisplayName("Should extract companyId correctly from token")
        void shouldExtractCompanyId() {
            String token = jwtTokenProvider.generateToken(testUserPrincipal);

            UUID extractedCompanyId = jwtTokenProvider.getCompanyIdFromToken(token);

            assertThat(extractedCompanyId).isEqualTo(companyId);
        }

        @Test
        @DisplayName("Should extract null companyId when token was generated for superadmin")
        void shouldExtractNullCompanyIdForSuperadmin() {
            UserPrincipal superAdmin = new UserPrincipal(
                    userId,
                    "superadmin@sepisac.com",
                    "superadmin",
                    "pwd",
                    null,
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_SUPERADMIN")),
                    true
            );
            String token = jwtTokenProvider.generateToken(superAdmin);

            UUID extractedCompanyId = jwtTokenProvider.getCompanyIdFromToken(token);

            assertThat(extractedCompanyId).isNull();
        }

        @Test
        @DisplayName("Should extract role correctly from token")
        void shouldExtractRole() {
            String token = jwtTokenProvider.generateToken(testUserPrincipal);

            String role = jwtTokenProvider.getRoleFromToken(token);

            assertThat(role).isEqualTo("ROLE_ADMIN_EMPRESA");
        }

        @Test
        @DisplayName("Should extract userId correctly from token subject")
        void shouldExtractUserId() {
            String token = jwtTokenProvider.generateToken(testUserPrincipal);

            UUID extractedUserId = jwtTokenProvider.getUserIdFromToken(token);

            assertThat(extractedUserId).isEqualTo(userId);
        }
    }
}
