package com.example.demo;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.sql.Date;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Spring Boot database configuration + small parsing utilities.
 *
 * IMPORTANT:
 * - Do NOT use DriverManager directly in a Spring Boot app.
 * - Inject DataSource/JdbcTemplate/Repositories where needed.
 *
 * Environment variables (recommended with Docker):
 *   SPRING_DATASOURCE_URL
 *   SPRING_DATASOURCE_USERNAME
 *   SPRING_DATASOURCE_PASSWORD
 *
 * Or set in application.yml / application.properties:
 *   spring.datasource.url
 *   spring.datasource.username
 *   spring.datasource.password
 */
@Configuration
public class Db {

    @Value("${spring.datasource.url:}")
    private String url;

    @Value("${spring.datasource.username:}")
    private String username;

    @Value("${spring.datasource.password:}")
    private String password;

    /**
     * DataSource bean (HikariCP). Spring will use this for all DB access.
     * Keep it if you want explicit control over pool settings.
     */
    @Bean
    public DataSource dataSource() {
        if (url == null || url.isBlank()) {
            throw new IllegalStateException(
                    "Missing DB config: set spring.datasource.url (or SPRING_DATASOURCE_URL).");
        }

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(username);
        config.setPassword(password);

        // Sensible defaults for a small app (tweak as needed)
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30_000);
        config.setIdleTimeout(600_000);
        config.setMaxLifetime(1_800_000);

        // MySQL driver hint (usually auto-detected)
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");

        return new HikariDataSource(config);
    }

    /**
     * Parse date from various formats (ISO and Excel-style)
     * Supports: yyyy-MM-dd, dd-MMM-yy, dd-MMM-yyyy, dd/MM/yyyy, dd-MM-yyyy
     *
     * @param input Date string
     * @return SQL Date object
     * @throws IllegalArgumentException if format is invalid
     */
    public static Date parseDate(String input) {
        if (input == null || input.trim().isEmpty()) {
            throw new IllegalArgumentException("Date cannot be null or empty");
        }

        input = input.trim();

        String[] patterns = {
                "yyyy-MM-dd",   // ISO: 1933-08-05
                "dd-MMM-yy",    // Excel: 08-Jan-56
                "dd-MMM-yyyy",  // Excel full: 08-Jan-1956
                "dd/MM/yyyy",   // European: 08/01/1956
                "dd-MM-yyyy"    // European dash: 08-01-1956
        };

        for (String pattern : patterns) {
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern, Locale.ENGLISH);
                LocalDate localDate = LocalDate.parse(input, formatter);

                // Fix century for 2-digit years
                if (pattern.equals("dd-MMM-yy") && localDate.getYear() > 2026) {
                    localDate = localDate.minusYears(100);
                }

                return Date.valueOf(localDate);

            } catch (Exception ignored) {
                // Try next pattern
            }
        }

        throw new IllegalArgumentException(
                "Invalid date format: '" + input + "'. " +
                        "Expected formats: yyyy-MM-dd, dd-MMM-yy, dd-MMM-yyyy, dd/MM/yyyy");
    }

    /**
     * Convert gender string to database integer
     * @param gender "M", "F", "Α" (male), "Θ" (female)
     * @return 0 for male, 1 for female
     */
    public static int parseGender(String gender) {
        if (gender == null || gender.trim().isEmpty()) {
            throw new IllegalArgumentException("Gender cannot be null or empty");
        }

        String g = gender.trim().toUpperCase();

        // Male: M, MALE, Α (Greek), 0
        if (g.equals("M") || g.equals("MALE") || g.equals("Α") || g.equals("0")) {
            return 0;
        }

        // Female: F, FEMALE, Θ (Greek), 1
        if (g.equals("F") || g.equals("FEMALE") || g.equals("Θ") || g.equals("1")) {
            return 1;
        }

        throw new IllegalArgumentException(
                "Invalid gender: '" + gender + "'. Use M/F, Male/Female, Α/Θ, or 0/1");
    }
}