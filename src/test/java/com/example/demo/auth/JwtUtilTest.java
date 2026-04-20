// ==========================================
// JwtUtilTest.java
// ==========================================
package com.example.demo.auth;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    // 256-bit key (32 bytes) — minimum for HMAC-SHA256
    private static final String SECRET = "this-is-a-test-secret-key-32bytes!";
    private static final long EXPIRATION_MS = 3600_000; // 1 hour

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil(SECRET, EXPIRATION_MS);
    }

    @Test
    void generateToken_andParseBack_returnsCorrectClaims() {
        String token = jwtUtil.generateToken("jim", "ROLE_ADMIN");

        assertEquals("jim", jwtUtil.getUsername(token));
        assertEquals("ROLE_ADMIN", jwtUtil.getRole(token));
    }

    @Test
    void isValid_validToken_returnsTrue() {
        String token = jwtUtil.generateToken("jim", "ROLE_DOCTOR");
        assertTrue(jwtUtil.isValid(token));
    }

    @Test
    void isValid_garbageToken_returnsFalse() {
        assertFalse(jwtUtil.isValid("not.a.real.token"));
    }

    @Test
    void isValid_tamperedToken_returnsFalse() {
        String token = jwtUtil.generateToken("jim", "ROLE_CLERK");
        // flip a character in the signature
        String tampered = token.substring(0, token.length() - 1) + "X";
        assertFalse(jwtUtil.isValid(tampered));
    }

    @Test
    void isValid_expiredToken_returnsFalse() {
        // Create a JwtUtil with 0ms expiration — token expires instantly
        JwtUtil expiredUtil = new JwtUtil(SECRET, 0);
        String token = expiredUtil.generateToken("jim", "ROLE_ADMIN");
        assertFalse(expiredUtil.isValid(token));
    }

    @Test
    void parseToken_containsExpectedFields() {
        String token = jwtUtil.generateToken("jim", "ROLE_DOCTOR");
        Claims claims = jwtUtil.parseToken(token);

        assertEquals("jim", claims.getSubject());
        assertEquals("ROLE_DOCTOR", claims.get("role", String.class));
        assertNotNull(claims.getIssuedAt());
        assertNotNull(claims.getExpiration());
    }
}