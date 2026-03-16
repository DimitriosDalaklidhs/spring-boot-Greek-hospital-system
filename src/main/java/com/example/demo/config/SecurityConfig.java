
package com.example.demo.config;

import com.example.demo.auth.JwtAuthFilter;
import com.example.demo.auth.UserDetailsServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final UserDetailsServiceImpl userDetailsService;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter,
                          UserDetailsServiceImpl userDetailsService) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.userDetailsService = userDetailsService;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .authenticationProvider(authenticationProvider())  // explicitly wire DaoAuthenticationProvider
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((req, res, e) -> {
                            res.setContentType("application/json");
                            res.setStatus(401);
                            res.getWriter().write("{\"error\":\"Unauthorized\",\"message\":\""
                                    + e.getMessage() + "\"}");
                        })
                        .accessDeniedHandler((req, res, e) -> {
                            res.setContentType("application/json");
                            res.setStatus(403);
                            res.getWriter().write("{\"error\":\"Forbidden\",\"message\":\""
                                    + e.getMessage() + "\"}");
                        })
                )
                .authorizeHttpRequests(auth -> auth

                        // Public login
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()

                        // Clerk atomic register: create patient + hospitalization
                        .requestMatchers(HttpMethod.POST, "/api/clerk/hospitalizations").hasRole("CLERK")

                        // Secure actuator endpoints
                        .requestMatchers("/actuator/**").authenticated()

                        // Auth management
                        .requestMatchers(HttpMethod.POST, "/api/auth/register").hasRole("ADMIN")

                        // Patients — specific rules BEFORE wildcard
                        .requestMatchers(HttpMethod.GET,  "/api/patients/*/tests").hasAnyRole("DOCTOR", "CLERK")
                        .requestMatchers(HttpMethod.POST, "/api/patients/*/tests").hasRole("DOCTOR")
                        .requestMatchers(HttpMethod.GET,  "/api/patients/**").hasAnyRole("DOCTOR", "CLERK")
                        .requestMatchers(HttpMethod.POST, "/api/patients").hasAnyRole("DOCTOR", "CLERK")

                        // Hospitalizations (doctor workflow)
                        .requestMatchers(HttpMethod.POST, "/api/hospitalizations").hasAnyRole("DOCTOR", "CLERK")
                        .requestMatchers(HttpMethod.PUT,  "/api/hospitalizations/*/discharge").hasRole("DOCTOR")

                        // Everything else requires auth
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
