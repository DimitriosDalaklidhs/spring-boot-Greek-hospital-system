package com.example.demo.auth;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Set;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    public record LoginRequest(
            @NotBlank String username,
            @NotBlank String password) {}

    public record LoginResponse(String token, String username, String role) {}

    public record RegisterRequest(
            @NotBlank @Size(min = 3, max = 100) String username,
            @NotBlank @Size(min = 8)            String password,
            @NotBlank                           String role) {}

    private static final Set<String> ALLOWED_ROLES =
            Set.of("ROLE_DOCTOR", "ROLE_CLERK", "ROLE_ADMIN");

    private final AuthenticationManager authManager;
    private final JwtUtil               jwtUtil;
    private final PasswordEncoder       passwordEncoder;
    private final JdbcTemplate          jdbc;

    public AuthController(AuthenticationManager authManager,
                          JwtUtil jwtUtil,
                          PasswordEncoder passwordEncoder,
                          JdbcTemplate jdbc) {
        this.authManager     = authManager;
        this.jwtUtil         = jwtUtil;
        this.passwordEncoder = passwordEncoder;
        this.jdbc            = jdbc;
    }

    // =========================
    // LOGIN
    // =========================
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest req) {
        try {
            Authentication auth = authManager.authenticate(
                    new UsernamePasswordAuthenticationToken(req.username(), req.password())
            );

            String role = auth.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .findFirst()
                    .orElse("ROLE_UNKNOWN");

            String token = jwtUtil.generateToken(auth.getName(), role);

            return new LoginResponse(token, auth.getName(), role);

        } catch (AuthenticationException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }
    }

    // =========================
    // REGISTER
    // =========================
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public void register(@Valid @RequestBody RegisterRequest req) {

        String role = req.role().trim().toUpperCase();

        if (!ALLOWED_ROLES.contains(role)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Role must be one of: ROLE_DOCTOR, ROLE_CLERK, ROLE_ADMIN"
            );
        }

        String sql = "INSERT INTO users (username, password, role) VALUES (?, ?, ?)";

        try {
            jdbc.update(
                    sql,
                    req.username(),
                    passwordEncoder.encode(req.password()),
                    role
            );
        } catch (org.springframework.dao.DuplicateKeyException e) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Username already exists: " + req.username()
            );
        }
    }
}