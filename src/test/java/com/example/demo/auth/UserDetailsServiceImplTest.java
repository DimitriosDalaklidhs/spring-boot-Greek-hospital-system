// ==========================================
// UserDetailsServiceImplTest.java
// ==========================================
package com.example.demo.auth;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserDetailsServiceImplTest {

    @Mock private JdbcTemplate jdbc;

    @InjectMocks private UserDetailsServiceImpl service;

    @Test
    void loadUserByUsername_existingUser_returnsUserDetails() {
        UserDetails mockUser = org.springframework.security.core.userdetails.User
                .withUsername("jim")
                .password("encoded-pass")
                .authorities("ROLE_ADMIN")
                .build();

        when(jdbc.queryForObject(anyString(), any(RowMapper.class), eq("jim")))
                .thenReturn(mockUser);

        UserDetails result = service.loadUserByUsername("jim");

        assertEquals("jim", result.getUsername());
        assertEquals("encoded-pass", result.getPassword());
        assertTrue(result.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
    }

    @Test
    void loadUserByUsername_unknownUser_throwsUsernameNotFound() {
        when(jdbc.queryForObject(anyString(), any(RowMapper.class), eq("ghost")))
                .thenThrow(new EmptyResultDataAccessException(1));

        assertThrows(
                UsernameNotFoundException.class,
                () -> service.loadUserByUsername("ghost")
        );
    }
}