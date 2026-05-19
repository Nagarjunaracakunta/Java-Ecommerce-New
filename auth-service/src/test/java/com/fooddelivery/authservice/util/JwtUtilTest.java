package com.fooddelivery.authservice.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    private static final String SECRET = "5367566B59703373367639792F423F4528482B4D6251655468576D5A71347437";

    @BeforeEach

    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", SECRET);
        ReflectionTestUtils.setField(jwtUtil, "expirationMs", 86400000L);
    }

    @Test
    void generateToken_shouldReturnNonBlankToken() {
        String token = jwtUtil.generateToken("alice", "ROLE_USER");
        assertThat(token).isNotBlank();
    }

    @Test
    void extractUsername_shouldReturnCorrectUsername() {
        String token = jwtUtil.generateToken("alice", "ROLE_USER");
        assertThat(jwtUtil.extractUsername(token)).isEqualTo("alice");
    }

    @Test
    void extractRole_shouldReturnCorrectRole() {
        String token = jwtUtil.generateToken("alice", "ROLE_ADMIN");
        assertThat(jwtUtil.extractRole(token)).isEqualTo("ROLE_ADMIN");
    }

    @Test
    void isTokenValid_shouldReturnTrueForValidToken() {
        String token = jwtUtil.generateToken("alice", "ROLE_USER");
        assertThat(jwtUtil.isTokenValid(token)).isTrue();
    }

    @Test
    void isTokenValid_shouldReturnFalseForTamperedToken() {
        String token = jwtUtil.generateToken("alice", "ROLE_USER");
        String tampered = token.substring(0, token.length() - 4) + "xxxx";
        assertThat(jwtUtil.isTokenValid(tampered)).isFalse();
    }

    @Test
    void isTokenValid_shouldReturnFalseForExpiredToken() {
        ReflectionTestUtils.setField(jwtUtil, "expirationMs", -1000L);
        String token = jwtUtil.generateToken("alice", "ROLE_USER");
        assertThat(jwtUtil.isTokenValid(token)).isFalse();
    }
}