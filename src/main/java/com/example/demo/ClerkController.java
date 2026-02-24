package com.example.demo;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/clerk")
public class ClerkController {

    private final ClerkService clerkService;

    public ClerkController(ClerkService clerkService) {
        this.clerkService = clerkService;
    }

    @PostMapping("/hospitalizations")
    @ResponseStatus(HttpStatus.CREATED)
    public ClerkService.RegisterResult register(@Valid @RequestBody RegisterHospitalizationRequest req) {
        return clerkService.registerPatientAndHospitalization(
                new ClerkService.RegisterRequest(
                        req.amka(),
                        req.firstName(),
                        req.lastName(),
                        req.birthDate(),
                        req.gender(),
                        req.hospitalId(),
                        req.admissionDate()
                )
        );
    }

    public record RegisterHospitalizationRequest(
            @NotBlank String amka,
            @NotBlank String firstName,
            @NotBlank String lastName,
            @NotBlank @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}", message = "birthDate must be yyyy-MM-dd")
            String birthDate,
            @NotBlank String gender,
            @NotNull Integer hospitalId,
            @NotBlank @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}", message = "admissionDate must be yyyy-MM-dd")
            String admissionDate
    ) {}
}