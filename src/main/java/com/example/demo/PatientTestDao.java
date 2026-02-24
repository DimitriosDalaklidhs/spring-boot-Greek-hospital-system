package com.example.demo;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

@Repository
public class PatientTestDao {

    private final JdbcTemplate jdbc;

    public PatientTestDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public int insert(int patientCode, int hospitalId, int testId, Date testDate) {
        String sql = """
            INSERT INTO ιατρικεσ_εξετασεισ_ασθενων
            (ΚΩΔ_ΑΣΘΕΝΗ, ΚΩΔ_ΝΟΣΟΚΟΜΕΙΟΥ, ΚΩΔ_ΙΑΤΡ_ΕΞΕΤΑΣΗΣ, ΗΜ_ΙΑΤΡ_ΕΞΕΤΑΣΗΣ)
            VALUES (?, ?, ?, ?)
        """;

        KeyHolder kh = new GeneratedKeyHolder();

        int rows = jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setInt(1, patientCode);
            ps.setInt(2, hospitalId);
            ps.setInt(3, testId);
            ps.setDate(4, testDate);
            return ps;
        }, kh);

        if (rows == 0) {
            throw new IllegalStateException("Insert test record failed, no rows affected");
        }

        Number key = kh.getKey();
        if (key == null) {
            throw new IllegalStateException("Insert test record failed, no generated key");
        }
        return key.intValue();
    }

    public boolean testExists(int testId) {
        String sql = "SELECT 1 FROM ιατρικεσ_εξετασεισ WHERE ΚΩΔ_ΙΑΤΡ_ΕΞΕΤΑΣΗΣ = ? LIMIT 1";
        Integer v = jdbc.query(
                con -> {
                    PreparedStatement ps = con.prepareStatement(sql);
                    ps.setInt(1, testId);
                    return ps;
                },
                rs -> rs.next() ? 1 : null
        );
        return v != null;
    }

    public Optional<TestInfo> getTestInfo(int testId) {
        String sql = """
            SELECT ΟΝΟΜΑΣΙΑ_ΙΑΤΡ_ΕΞΕΤΑΣΗΣ, ΚΟΣΤΟΣ_ΙΑΤΡ_ΕΞΕΤΑΣΗΣ
            FROM ιατρικεσ_εξετασεισ
            WHERE ΚΩΔ_ΙΑΤΡ_ΕΞΕΤΑΣΗΣ = ?
        """;

        try {
            TestInfo info = jdbc.queryForObject(sql, (rs, rowNum) ->
                    new TestInfo(
                            rs.getString("ΟΝΟΜΑΣΙΑ_ΙΑΤΡ_ΕΞΕΤΑΣΗΣ"),
                            rs.getBigDecimal("ΚΟΣΤΟΣ_ΙΑΤΡ_ΕΞΕΤΑΣΗΣ")
                    ), testId);

            return Optional.ofNullable(info);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public List<PatientTestHistoryItem> getPatientTests(int patientCode) {
        String sql = """
            SELECT
                t.ΗΜ_ΙΑΤΡ_ΕΞΕΤΑΣΗΣ,
                e.ΟΝΟΜΑΣΙΑ_ΙΑΤΡ_ΕΞΕΤΑΣΗΣ,
                n.ΟΝΟΜΑΣΙΑ_ΝΟΣΟΚΟΜΕΙΟΥ,
                e.ΚΟΣΤΟΣ_ΙΑΤΡ_ΕΞΕΤΑΣΗΣ
            FROM ιατρικεσ_εξετασεισ_ασθενων t
            JOIN ιατρικεσ_εξετασεισ e ON t.ΚΩΔ_ΙΑΤΡ_ΕΞΕΤΑΣΗΣ = e.ΚΩΔ_ΙΑΤΡ_ΕΞΕΤΑΣΗΣ
            JOIN νοσοκομεια n ON t.ΚΩΔ_ΝΟΣΟΚΟΜΕΙΟΥ = n.ΚΩΔ_ΝΟΣΟΚΟΜΕΙΟΥ
            WHERE t.ΚΩΔ_ΑΣΘΕΝΗ = ?
            ORDER BY t.ΗΜ_ΙΑΤΡ_ΕΞΕΤΑΣΗΣ DESC
        """;

        return jdbc.query(sql, (rs, rowNum) -> new PatientTestHistoryItem(
                rs.getDate("ΗΜ_ΙΑΤΡ_ΕΞΕΤΑΣΗΣ"),
                rs.getString("ΟΝΟΜΑΣΙΑ_ΙΑΤΡ_ΕΞΕΤΑΣΗΣ"),
                rs.getString("ΟΝΟΜΑΣΙΑ_ΝΟΣΟΚΟΜΕΙΟΥ"),
                rs.getBigDecimal("ΚΟΣΤΟΣ_ΙΑΤΡ_ΕΞΕΤΑΣΗΣ")
        ), patientCode);
    }

    public record TestInfo(String name, java.math.BigDecimal costEur) {}

    public record PatientTestHistoryItem(
            Date testDate,
            String testName,
            String hospitalName,
            java.math.BigDecimal costEur
    ) {}
}