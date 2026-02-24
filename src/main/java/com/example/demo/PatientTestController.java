package com.example.demo;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/patients/{amka}/tests")
public class PatientTestController {

    private final DoctorService doctorService;

    public PatientTestController(DoctorService doctorService) {
        this.doctorService = doctorService;
    }

    @GetMapping
    public List<PatientTestDao.PatientTestHistoryItem> getHistory(@PathVariable String amka) {
        return doctorService.getPatientTestHistory(amka);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreateTestResponse addTest(@PathVariable String amka,
                                      @Valid @RequestBody AddTestBody body) {

        int id = doctorService.addPatientTest(new DoctorService.AddPatientTestRequest(
                amka,
                body.hospitalId(),
                body.testId(),
                body.testDate()
        ));

        return new CreateTestResponse(id);
    }

    public record AddTestBody(
            @Min(1) int hospitalId,
            @Min(1) int testId,
            String testDate
    ) {}

    public record CreateTestResponse(int recordId) {}
}