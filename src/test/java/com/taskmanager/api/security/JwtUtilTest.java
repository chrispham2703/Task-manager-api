package com.taskmanager.api.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("JwtUtil")
class JwtUtilTest {

    private JwtUtil jwtUtil;
    private final String secret = "mySecretKeyForJWTTokenGenerationMustBeAtLeast256BitsLongForSecurityTesting";
    private final long accessExpirationMs = 3600000; // 1 hour
    private final long refreshExpirationMs = 604800000; // 7 days

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil(secret, accessExpirationMs, refreshExpirationMs);
    }

    @Nested
    @DisplayName("generateAccessToken")
    class GenerateAccessToken {

        @Test
        @DisplayName("should generate valid access token")
        void shouldGenerateValidAccessToken() {
            // Given
            UUID userId = UUID.randomUUID();
            String email = "test@example.com";

            // When
            String token = jwtUtil.generateAccessToken(userId, email);

            // Then
            assertThat(token).isNotNull().isNotEmpty();
            assertThat(jwtUtil.validateToken(token)).isTrue();
            assertThat(jwtUtil.isAccessToken(token)).isTrue();
            assertThat(jwtUtil.isRefreshToken(token)).isFalse();
        }

        @Test
        @DisplayName("should include user ID and email in token")
        void shouldIncludeUserInfo() {
            // Given
            UUID userId = UUID.randomUUID();
            String email = "test@example.com";

            // When
            String token = jwtUtil.generateAccessToken(userId, email);

            // Then
            assertThat(jwtUtil.getUserIdFromToken(token)).isEqualTo(userId);
            assertThat(jwtUtil.getEmailFromToken(token)).isEqualTo(email);
        }
    }

    @Nested
    @DisplayName("generateRefreshToken")
    class GenerateRefreshToken {

        @Test
        @DisplayName("should generate valid refresh token")
        void shouldGenerateValidRefreshToken() {
            // Given
            UUID userId = UUID.randomUUID();
            String email = "test@example.com";

            // When
            String token = jwtUtil.generateRefreshToken(userId, email);

            // Then
            assertThat(token).isNotNull().isNotEmpty();
            assertThat(jwtUtil.validateToken(token)).isTrue();
            assertThat(jwtUtil.isRefreshToken(token)).isTrue();
            assertThat(jwtUtil.isAccessToken(token)).isFalse();
        }
    }

    @Nested
    @DisplayName("validateToken")
    class ValidateToken {

        @Test
        @DisplayName("should return true for valid token")
        void shouldReturnTrueForValidToken() {
            // Given
            String token = jwtUtil.generateAccessToken(UUID.randomUUID(), "test@example.com");

            // When
            boolean isValid = jwtUtil.validateToken(token);

            // Then
            assertThat(isValid).isTrue();
        }

        @Test
        @DisplayName("should return false for invalid token")
        void shouldReturnFalseForInvalidToken() {
            // When
            boolean isValid = jwtUtil.validateToken("invalid.token.here");

            // Then
            assertThat(isValid).isFalse();
        }

        @Test
        @DisplayName("should return false for empty token")
        void shouldReturnFalseForEmptyToken() {
            // When
            boolean isValid = jwtUtil.validateToken("");

            // Then
            assertThat(isValid).isFalse();
        }

        @Test
        @DisplayName("should return false for tampered token")
        void shouldReturnFalseForTamperedToken() {
            // Given
            String token = jwtUtil.generateAccessToken(UUID.randomUUID(), "test@example.com");
            String tamperedToken = token.substring(0, token.length() - 5) + "XXXXX";

            // When
            boolean isValid = jwtUtil.validateToken(tamperedToken);

            // Then
            assertThat(isValid).isFalse();
        }
    }

    @Nested
    @DisplayName("token expiration")
    class TokenExpiration {

        @Test
        @DisplayName("should correctly report access token expiration time")
        void shouldReportAccessTokenExpiration() {
            // When
            long expirationMs = jwtUtil.getAccessTokenExpirationMs();

            // Then
            assertThat(expirationMs).isEqualTo(accessExpirationMs);
        }

        @Test
        @DisplayName("should correctly report refresh token expiration time")
        void shouldReportRefreshTokenExpiration() {
            // When
            long expirationMs = jwtUtil.getRefreshTokenExpirationMs();

            // Then
            assertThat(expirationMs).isEqualTo(refreshExpirationMs);
        }
    }

    @Nested
    @DisplayName("backward compatibility")
    class BackwardCompatibility {

        @Test
        @DisplayName("generateToken should work as generateAccessToken")
        void generateTokenShouldWorkAsAccessToken() {
            // Given
            UUID userId = UUID.randomUUID();
            String email = "test@example.com";

            // When
            String token = jwtUtil.generateToken(userId, email);

            // Then
            assertThat(jwtUtil.validateToken(token)).isTrue();
            assertThat(jwtUtil.isAccessToken(token)).isTrue();
        }

        @Test
        @DisplayName("getExpirationMs should return access token expiration")
        void getExpirationMsShouldReturnAccessTokenExpiration() {
            // When
            long expirationMs = jwtUtil.getExpirationMs();

            // Then
            assertThat(expirationMs).isEqualTo(accessExpirationMs);
        }
    }
}
