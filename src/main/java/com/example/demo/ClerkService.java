package com.example.demo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;

@Service
public class ClerkService {

    private final PatientDao patientDao;
    private final HospitalizationDao hospitalizationDao;

    public ClerkService(PatientDao patientDao, HospitalizationDao hospitalizationDao) {
        this.patientDao = patientDao;
        this.hospitalizationDao = hospitalizationDao;
    }

    /**
     * Register patient and hospitalization atomically.
     * In Spring, @Transactional instead of manual commit or rollback.
     */
    @Transactional
    public RegisterResult registerPatientAndHospitalization(RegisterRequest req) {
        // Validate AMKA
        AmkaValidator.validateOrThrow(req.amka());

        // Parse input
        Date birthDate = Db.parseDate(req.birthDate());
        Date admissionDate = Db.parseDate(req.admissionDate());
        int gender = Db.parseGender(req.gender());

        // Verify hospital exists
        if (!hospitalizationDao.hospitalExists(req.hospitalId())) {
            throw new IllegalArgumentException("Μη έγκυρος κωδικός νοσοκομείου: " + req.hospitalId());
        }

        // Step 1: find or create patient
        int patientCode = patientDao.getPatientCode(req.amka())
                .orElseGet(() -> patientDao.insert(req.amka(), req.firstName(), req.lastName(), birthDate, gender));

        // Step 2: create hospitalization
        int hospitalizationId = hospitalizationDao.insert(patientCode, req.hospitalId(), admissionDate);

        return new RegisterResult(patientCode, hospitalizationId);
    }

    public record RegisterRequest(
            String amka,
            String firstName,
            String lastName,
            String birthDate,
            String gender,
            int hospitalId,
            String admissionDate
    ) {}

    public record RegisterResult(int patientCode, int hospitalizationId) {}
}
