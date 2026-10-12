package com.employeehub.security;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JwtUtilsTest {

    private final JwtUtils jwtUtils = new JwtUtils("test-only-secret-key-that-is-at-least-32-bytes-long", 60_000);

    @Test
    void generatedTokenIsValidAndCarriesUsername() {
        String token = jwtUtils.generateToken("alice", List.of("ROLE_ADMIN"));

        assertThat(jwtUtils.validateToken(token)).isTrue();
        assertThat(jwtUtils.getUsername(token)).isEqualTo("alice");
    }

    @Test
    void tokenSignedWithDifferentSecretIsRejected() {
        JwtUtils other = new JwtUtils("a-completely-different-secret-key-of-32-bytes!", 60_000);
        String token = other.generateToken("mallory", List.of("ROLE_ADMIN"));

        assertThat(jwtUtils.validateToken(token)).isFalse();
    }

    @Test
    void expiredTokenIsRejected() {
        JwtUtils shortLived = new JwtUtils("test-only-secret-key-that-is-at-least-32-bytes-long", -1_000);
        String token = shortLived.generateToken("alice", List.of("ROLE_ADMIN"));

        assertThat(jwtUtils.validateToken(token)).isFalse();
    }

    @Test
    void garbageTokenIsRejected() {
        assertThat(jwtUtils.validateToken("not-a-jwt")).isFalse();
    }
}
