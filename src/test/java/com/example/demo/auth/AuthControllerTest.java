// ==========================================
// AuthControllerTest.java
// ==========================================
package com.example.demo.auth;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock private AuthenticationManager authManager;
    @Mock private JwtUtil jwtUtil;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JdbcTemplate jdbc;

    @InjectMocks private AuthController controller;

    // ---- LOGIN ----

    @Test
    void login_validCredentials_returnsTokenAndRole() {
        var req = new AuthController.LoginRequest("jim", "password123");

        Authentication auth = new UsernamePasswordAuthenticationToken(
                "jim", null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );
        when(authManager.authenticate(any())).thenReturn(auth);
        when(jwtUtil.generateToken("jim", "ROLE_ADMIN")).thenReturn("jwt-token-123");

        AuthController.LoginResponse resp = controller.login(req);

        assertEquals("jwt-token-123", resp.token());
        assertEquals("jim", resp.username());
        assertEquals("ROLE_ADMIN", resp.role());
    }

    @Test
    void login_invalidCredentials_throws401() {
        var req = new AuthController.LoginRequest("jim", "wrong");
        when(authManager.authenticate(any())).thenThrow(new BadCredentialsException("bad"));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class, () -> controller.login(req)
        );
        assertEquals(401, ex.getStatusCode().value());
    }

    @Test
    void login_noAuthorities_defaultsToRoleUnknown() {
        var req = new AuthController.LoginRequest("jim", "pass");

        // Authentication with empty authorities list
        Authentication auth = new UsernamePasswordAuthenticationToken("jim", null, List.of());
        when(authManager.authenticate(any())).thenReturn(auth);
        when(jwtUtil.generateToken("jim", "ROLE_UNKNOWN")).thenReturn("token");

        AuthController.LoginResponse resp = controller.login(req);
        assertEquals("ROLE_UNKNOWN", resp.role());
    }

    // ---- REGISTER ----

    @Test
    void register_validDoctor_insertsWithEncodedPassword() {
        var req = new AuthController.RegisterRequest("newdoc", "securepass", "ROLE_DOCTOR");
        when(passwordEncoder.encode("securepass")).thenReturn("encoded-hash");

        controller.register(req);

        verify(jdbc).update(
                eq("INSERT INTO users (username, password, role) VALUES (?, ?, ?)"),
                eq("newdoc"),
                eq("encoded-hash"),
                eq("ROLE_DOCTOR")
        );
    }

    @Test
    void register_roleIsCaseInsensitive() {
        var req = new AuthController.RegisterRequest("user1", "securepass", "role_clerk");
        when(passwordEncoder.encode(any())).thenReturn("hash");

        controller.register(req);

        verify(jdbc).update(anyString(), eq("user1"), eq("hash"), eq("ROLE_CLERK"));
    }

    @Test
    void register_roleTrimmed() {
        var req = new AuthController.RegisterRequest("user1", "securepass", "  ROLE_ADMIN  ");
        when(passwordEncoder.encode(any())).thenReturn("hash");

        controller.register(req);

        verify(jdbc).update(anyString(), eq("user1"), eq("hash"), eq("ROLE_ADMIN"));
    }

    @Test
    void register_invalidRole_throws400() {
        var req = new AuthController.RegisterRequest("user1", "pass1234", "ROLE_HACKER");

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class, () -> controller.register(req)
        );
        assertEquals(400, ex.getStatusCode().value());
        assertTrue(ex.getReason().contains("ROLE_DOCTOR"));
    }

    @Test
    void register_duplicateUsername_throws409() {
        var req = new AuthController.RegisterRequest("existing", "pass1234", "ROLE_CLERK");
        when(passwordEncoder.encode(any())).thenReturn("hash");
        when(jdbc.update(anyString(), any(), any(), any()))
                .thenThrow(new DuplicateKeyException("duplicate"));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class, () -> controller.register(req)
        );
        assertEquals(409, ex.getStatusCode().value());
        assertTrue(ex.getReason().contains("existing"));
    }
}