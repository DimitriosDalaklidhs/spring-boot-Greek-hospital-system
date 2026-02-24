package com.example.demo;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;

@Service
public class DoctorService {

    private final PatientDao patientDao;
    private final PatientTestDao patientTestDao;
    private final HospitalizationDao hospitalizationDao;

    public DoctorService(PatientDao patientDao,
                         PatientTestDao patientTestDao,
                         HospitalizationDao hospitalizationDao) {
        this.patientDao = patientDao;
        this.patientTestDao = patientTestDao;
        this.hospitalizationDao = hospitalizationDao;
    }

    public PatientDao.PatientInfo getPatientByAmka(String amka) {
        requireValidAmka(amka);

        return patientDao.getPatientInfo(amka)
                .orElseThrow(() -> new NotFoundException("Δεν βρέθηκε ασθενής με ΑΜΚΑ: " + amka));
    }

    @Transactional
    public PatientDao.PatientInfo createPatientIfNotExists(CreatePatientRequest req) {
        requireValidAmka(req.amka());

        return patientDao.getPatientInfo(req.amka())
                .orElseGet(() -> {
                    int gender = Db.parseGender(req.gender());
                    Date birth = Db.parseDate(req.birthDate());
                    patientDao.insert(req.amka(), req.firstName(), req.lastName(), birth, gender);
                    return patientDao.getPatientInfo(req.amka())
                            .orElseThrow(() -> new IllegalStateException("Αποτυχία δημιουργίας ασθενή"));
                });
    }

    public List<PatientTestDao.PatientTestHistoryItem> getPatientTestHistory(String amka) {
        requireValidAmka(amka);

        int patientCode = patientDao.getPatientCode(amka)
                .orElseThrow(() -> new NotFoundException("Δεν βρέθηκε ασθενής με ΑΜΚΑ: " + amka));

        return patientTestDao.getPatientTests(patientCode);
    }

    @Transactional
    public int addPatientTest(AddPatientTestRequest req) {
        requireValidAmka(req.amka());

        int patientCode = patientDao.getPatientCode(req.amka())
                .orElseThrow(() -> new NotFoundException("Δεν βρέθηκε ασθενής με ΑΜΚΑ: " + req.amka()));

        if (!hospitalizationDao.hospitalExists(req.hospitalId())) {
            throw new BadRequestException("Μη έγκυρος κωδικός νοσοκομείου: " + req.hospitalId());
        }
        if (!patientTestDao.testExists(req.testId())) {
            throw new BadRequestException("Μη έγκυρος κωδικός ιατρικής εξέτασης: " + req.testId());
        }

        Date testDate = req.testDate() == null
                ? Date.valueOf(LocalDate.now())
                : Db.parseDate(req.testDate());

        return patientTestDao.insert(patientCode, req.hospitalId(), req.testId(), testDate);
    }

    @Transactional
    public int hospitalizePatient(HospitalizePatientRequest req) {
        requireValidAmka(req.amka());

        int patientCode = patientDao.getPatientCode(req.amka())
                .orElseThrow(() -> new NotFoundException("Δεν βρέθηκε ασθενής με ΑΜΚΑ: " + req.amka()));

        if (!hospitalizationDao.hospitalExists(req.hospitalId())) {
            throw new BadRequestException("Μη έγκυρος κωδικός νοσοκομείου: " + req.hospitalId());
        }

        Date admission = req.admissionDate() == null
                ? Date.valueOf(LocalDate.now())
                : Db.parseDate(req.admissionDate());

        return hospitalizationDao.insert(patientCode, req.hospitalId(), admission);
    }

    @Transactional
    public void discharge(int hospitalizationId, String dischargeDate) {
        Date d = (dischargeDate == null || dischargeDate.isBlank())
                ? Date.valueOf(LocalDate.now())
                : Db.parseDate(dischargeDate);

        hospitalizationDao.updateDischargeDate(hospitalizationId, d);
    }

    // -----------------------
    // Helpers + DTOs + errors
    // -----------------------

    private static void requireValidAmka(String amka) {
        if (amka == null || amka.isBlank()) {
            throw new BadRequestException("Το ΑΜΚΑ είναι υποχρεωτικό.");
        }
        if (!AmkaValidator.isValid(amka)) {
            throw new BadRequestException("Μη έγκυρο ΑΜΚΑ: " + amka);
        }
    }

    public record CreatePatientRequest(
            @NotBlank String amka,
            @NotBlank String firstName,
            @NotBlank String lastName,
            @NotBlank String birthDate,
            @NotBlank String gender
    ) {}

    public record AddPatientTestRequest(
            @NotBlank String amka,
            @Min(1) int hospitalId,
            @Min(1) int testId,
            String testDate
    ) {}

    public record HospitalizePatientRequest(
            @NotBlank String amka,
            @Min(1) int hospitalId,
            String admissionDate
    ) {}

    public static class NotFoundException extends RuntimeException {
        public NotFoundException(String message) { super(message); }
    }

    public static class BadRequestException extends RuntimeException {
        public BadRequestException(String message) { super(message); }
    }
}