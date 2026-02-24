package com.example.demo;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/hospitalizations")
public class HospitalizationController {

    private final DoctorService doctorService;

    public HospitalizationController(DoctorService doctorService) {
        this.doctorService = doctorService;
    }

    @PostMapping
    public int hospitalize(@Valid @RequestBody DoctorService.HospitalizePatientRequest request) {
        return doctorService.hospitalizePatient(request);
    }

    @PutMapping("/{id}/discharge")
    public void discharge(@PathVariable int id,
                          @RequestParam(required = false) String date) {
        doctorService.discharge(id, date);
    }
}