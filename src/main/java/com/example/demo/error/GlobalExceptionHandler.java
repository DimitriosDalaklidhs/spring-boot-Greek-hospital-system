package com.example.demo.error;

import com.example.demo.DoctorService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    public record ApiError(String code, String message, int status, OffsetDateTime timestamp) {}

    //  Keep the original HTTP status (401, 400, 403, etc.)
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiError> handleResponseStatus(ResponseStatusException ex) {
        int status = ex.getStatusCode().value();
        String code = ex.getStatusCode().toString(); // e.g. "401 UNAUTHORIZED"
        String message = ex.getReason() != null ? ex.getReason() : ex.getMessage();

        return ResponseEntity.status(status).body(
                new ApiError(code, message, status, OffsetDateTime.now())
        );
    }

    //  Validation errors -> 400
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
        String msg = ex.getBindingResult().getAllErrors().stream()
                .map(err -> {
                    if (err instanceof FieldError fe) {
                        return fe.getField() + ": " + fe.getDefaultMessage();
                    }
                    return err.getDefaultMessage();
                })
                .collect(Collectors.joining(", "));

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                new ApiError("BAD_REQUEST", msg, 400, OffsetDateTime.now())
        );
    }

    //  Spring "ErrorResponseException" also carries an HTTP status
    @ExceptionHandler(ErrorResponseException.class)
    public ResponseEntity<ApiError> handleErrorResponse(ErrorResponseException ex) {
        int status = ex.getStatusCode().value();
        return ResponseEntity.status(status).body(
                new ApiError(ex.getStatusCode().toString(), ex.getMessage(), status, OffsetDateTime.now())
        );
    }

    //  Domain: resource not found -> 404
    @ExceptionHandler(DoctorService.NotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(DoctorService.NotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                new ApiError("NOT_FOUND", ex.getMessage(), 404, OffsetDateTime.now())
        );
    }

    //  Domain: bad caller input -> 400
    @ExceptionHandler(DoctorService.BadRequestException.class)
    public ResponseEntity<ApiError> handleBadRequest(DoctorService.BadRequestException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                new ApiError("BAD_REQUEST", ex.getMessage(), 400, OffsetDateTime.now())
        );
    }

    //  IllegalArgumentException (e.g. from ClerkService / Db utils) -> 400
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                new ApiError("BAD_REQUEST", ex.getMessage(), 400, OffsetDateTime.now())
        );
    }

    //  Fallback -> 500
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleAny(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                new ApiError("INTERNAL_ERROR", "Παρουσιάστηκε σφάλμα στον διακομιστή.", 500, OffsetDateTime.now())
        );
    }
}