package com.example.demo;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/patients")
public class PatientController {

    private final DoctorService doctorService;

    public PatientController(DoctorService doctorService) {
        this.doctorService = doctorService;
    }

    @GetMapping("/{amka}")
    public PatientDao.PatientInfo getByAmka(@PathVariable String amka) {
        return doctorService.getPatientByAmka(amka);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PatientDao.PatientInfo create(@Valid @RequestBody DoctorService.CreatePatientRequest request) {
        return doctorService.createPatientIfNotExists(request);
    }
}