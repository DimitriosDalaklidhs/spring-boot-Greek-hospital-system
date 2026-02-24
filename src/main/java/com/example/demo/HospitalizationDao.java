package com.example.demo;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Optional;

@Repository
public class HospitalizationDao {

    private final JdbcTemplate jdbc;

    public HospitalizationDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public int insert(int patientCode, int hospitalId, Date admissionDate) {
        String sql = """
            INSERT INTO νοσηλειεσ_ασθενων (ΚΩΔ_ΑΣΘΕΝΗ, ΚΩΔ_ΝΟΣΟΚΟΜΕΙΟΥ, ΗΜ_ΕΙΣΟΔΟΥ)
            VALUES (?, ?, ?)
        """;

        KeyHolder kh = new GeneratedKeyHolder();

        int rows = jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setInt(1, patientCode);
            ps.setInt(2, hospitalId);
            ps.setDate(3, admissionDate);
            return ps;
        }, kh);

        if (rows == 0) {
            throw new IllegalStateException("Insert hospitalization failed, no rows affected");
        }

        Number key = kh.getKey();
        if (key == null) {
            throw new IllegalStateException("Insert hospitalization failed, no generated key");
        }
        return key.intValue();
    }

    public boolean hospitalExists(int hospitalId) {
        String sql = "SELECT 1 FROM νοσοκομεια WHERE ΚΩΔ_ΝΟΣΟΚΟΜΕΙΟΥ = ? LIMIT 1";
        Integer v = jdbc.query(
                con -> {
                    PreparedStatement ps = con.prepareStatement(sql);
                    ps.setInt(1, hospitalId);
                    return ps;
                },
                rs -> rs.next() ? 1 : null
        );
        return v != null;
    }

    public Optional<String> getHospitalName(int hospitalId) {
        String sql = "SELECT ΟΝΟΜΑΣΙΑ_ΝΟΣΟΚΟΜΕΙΟΥ FROM νοσοκομεια WHERE ΚΩΔ_ΝΟΣΟΚΟΜΕΙΟΥ = ?";
        try {
            String name = jdbc.queryForObject(sql, String.class, hospitalId);
            return Optional.ofNullable(name);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public void updateDischargeDate(int hospitalizationId, Date dischargeDate) {
        String sql = """
            UPDATE νοσηλειεσ_ασθενων
            SET ΗΜ_ΕΞΟΔΟΥ = ?
            WHERE ΑΑ_ΝΟΣΗΛΕΙΑΣ = ?
        """;

        int rows = jdbc.update(sql, dischargeDate, hospitalizationId);
        if (rows == 0) {
            throw new com.example.demo.DoctorService.NotFoundException(
                    "Hospitalization not found: " + hospitalizationId
            );
        }
    }
}