package com.example.demo;

import java.time.DateTimeException;
import java.time.LocalDate;

/**
 * AMKA Validator (Luhn algorithm as described in the Excel file)
 *
 * AMKA Structure (is 11 digits):
 * - Positions 1-6: Birth date (DDMMYY)
 * - Positions 7-10: Sequential number
 * - Position 11: Check digit (Luhn)
 *
 * Luhn rule used here:
 * - Double digits at positions 2,4,6,8,10 (from the LEFT)
 * - If doubled value >= 10, subtract 9
 * - Sum all processed digits (including check digit)
 * - Valid if sum % 10 == 0
 */
public final class AmkaValidator {

    private AmkaValidator() {}

    public static boolean isValid(String amka) {
        if (amka == null || amka.length() != 11) return false;
        if (!amka.matches("\\d{11}")) return false;

        int sum = 0;
        for (int i = 0; i < 11; i++) {
            int d = amka.charAt(i) - '0';
            int pos = i + 1; // 1..11 from left

            if (pos % 2 == 0) {
                d *= 2;
                if (d >= 10) d -= 9;
            }
            sum += d;
        }
        return sum % 10 == 0;
    }

    public static void validateOrThrow(String amka) throws IllegalArgumentException {
        if (amka == null) {
            throw new IllegalArgumentException("AMKA cannot be null");
        }

        if (amka.length() != 11) {
            throw new IllegalArgumentException(
                    "AMKA must be exactly 11 digits (found " + amka.length() + ")");
        }

        if (!amka.matches("\\d{11}")) {
            throw new IllegalArgumentException("AMKA must contain only digits");
        }

        if (!isValid(amka)) {
            throw new IllegalArgumentException("AMKA Luhn validation failed: " + amka);
        }
    }

    /**
     * Extracts birth date components from AMKA (DDMMYY).
     * Returns null if AMKA is not Luhn-valid OR date is not plausible.
     */
    public static int[] extractBirthDate(String amka) {
        if (!isValid(amka)) return null;

        try {
            int day = Integer.parseInt(amka.substring(0, 2));
            int month = Integer.parseInt(amka.substring(2, 4));
            int year2 = Integer.parseInt(amka.substring(4, 6));

            int pivot = LocalDate.now().getYear() % 100; // e.g., 26 for 2026
            int fullYear = (year2 <= pivot) ? 2000 + year2 : 1900 + year2;

            LocalDate.of(fullYear, month, day);

            return new int[]{day, month, fullYear};
        } catch (NumberFormatException | DateTimeException e) {
            return null;
        }
    }
}
