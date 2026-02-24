package com.example.demo.auth;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final JdbcTemplate jdbc;

    public UserDetailsServiceImpl(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        String sql = "SELECT username, password, role FROM users WHERE username = ?";
        try {
            return jdbc.queryForObject(sql, (rs, rowNum) -> {
                String uname    = rs.getString("username");
                String password = rs.getString("password");
                String role     = rs.getString("role");
                return new User(uname, password, List.of(new SimpleGrantedAuthority(role)));
            }, username);
        } catch (EmptyResultDataAccessException e) {
            throw new UsernameNotFoundException("User not found: " + username);
        }
    }
}