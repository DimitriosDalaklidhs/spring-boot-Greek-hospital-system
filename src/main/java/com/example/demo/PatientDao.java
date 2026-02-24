package com.example.demo;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Objects;
import java.util.Optional;

@Repository
public class PatientDao {

    private final JdbcTemplate jdbc;

    public PatientDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<Integer> getPatientCode(String amka) {
        String sql = "SELECT ΚΩΔ_ΑΣΘΕΝΗ FROM ασθενεισ WHERE ΑΜΚΑ = ?";
        try {
            Integer code = jdbc.queryForObject(sql, Integer.class, amka);
            return Optional.ofNullable(code);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    /**
     * Minimal insert that matches NOT NULL columns.
     * Optional columns are left NULL.
     */
    public int insert(String amka,
                      String firstName,
                      String lastName,
                      Date birthDate,
                      int gender) {

        String sql = """
            INSERT INTO ασθενεισ (ΑΜΚΑ, ΟΝΟΜΑ, ΕΠΩΝΥΜΟ, ΗΜ_ΓΕΝΝΗΣΗΣ, ΦΥΛΟ)
            VALUES (?, ?, ?, ?, ?)
        """;

        KeyHolder kh = new GeneratedKeyHolder();

        int rows = jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, amka);
            ps.setString(2, firstName);
            ps.setString(3, lastName);
            ps.setDate(4, birthDate);
            ps.setInt(5, gender);
            return ps;
        }, kh);

        if (rows == 0) {
            throw new IllegalStateException("Insert patient failed, no rows affected");
        }

        Number key = kh.getKey();
        if (key == null) {
            throw new IllegalStateException("Insert patient failed, no generated key");
        }
        return key.intValue();
    }

    public Optional<PatientInfo> getPatientInfo(String amka) {
        String sql = """
            SELECT ΚΩΔ_ΑΣΘΕΝΗ, ΟΝΟΜΑ, ΕΠΩΝΥΜΟ, ΗΜ_ΓΕΝΝΗΣΗΣ, ΦΥΛΟ, ΑΜΚΑ
            FROM ασθενεισ
            WHERE ΑΜΚΑ = ?
        """;
        try {
            PatientInfo info = jdbc.queryForObject(sql, (rs, rowNum) ->
                    new PatientInfo(
                            rs.getInt("ΚΩΔ_ΑΣΘΕΝΗ"),
                            rs.getString("ΟΝΟΜΑ"),
                            rs.getString("ΕΠΩΝΥΜΟ"),
                            rs.getDate("ΗΜ_ΓΕΝΝΗΣΗΣ"),
                            rs.getInt("ΦΥΛΟ"),
                            rs.getString("ΑΜΚΑ")
                    ), amka);

            return Optional.ofNullable(info);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public record PatientInfo(
            int code,
            String firstName,
            String lastName,
            Date birthDate,
            int gender,
            String amka
    ) {
        public String genderLabel() {
            return gender == 0 ? "M" : "F";
        }

        @Override
        public String toString() {
            return "Patient[code=%d, name=%s %s, birth=%s, gender=%s, amka=%s]".formatted(
                    code,
                    Objects.toString(firstName, ""),
                    Objects.toString(lastName, ""),
                    birthDate,
                    genderLabel(),
                    amka
            );
        }
    }
}