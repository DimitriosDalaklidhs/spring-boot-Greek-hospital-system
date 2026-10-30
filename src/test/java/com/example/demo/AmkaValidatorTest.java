package com.example.demo;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AmkaValidatorTest {

    // Luhn-valid ΑΜΚΑ numbers (DDMMYY + 4-digit sequence + check digit)
    private static final String VALID_1950 = "01015012345"; // 01/01/1950
    private static final String VALID_2005 = "15030512345"; // 15/03/2005
    private static final String VALID_2000 = "29080012346"; // 29/08/2000
    private static final String VALID_1999 = "12129912346"; // 12/12/1999

    // ---- isValid ----

    @Test
    void isValid_knownValidAmkas_returnTrue() {
        assertTrue(AmkaValidator.isValid(VALID_1950));
        assertTrue(AmkaValidator.isValid(VALID_2005));
        assertTrue(AmkaValidator.isValid(VALID_2000));
        assertTrue(AmkaValidator.isValid(VALID_1999));
    }

    @Test
    void isValid_wrongCheckDigit_returnsFalse() {
        assertFalse(AmkaValidator.isValid("01015012346"));
    }

    @Test
    void isValid_anySingleDigitTypo_returnsFalse() {
        // Luhn catches every single-digit typo: change one digit, the number must fail
        char[] digits = VALID_1950.toCharArray();
        for (int pos = 0; pos < digits.length; pos++) {
            char original = digits[pos];
            for (char d = '0'; d <= '9'; d++) {
                if (d == original) continue;
                digits[pos] = d;
                String typo = new String(digits);
                assertFalse(AmkaValidator.isValid(typo), "Typo not caught: " + typo);
            }
            digits[pos] = original;
        }
    }

    @Test
    void isValid_null_returnsFalse() {
        assertFalse(AmkaValidator.isValid(null));
    }

    @Test
    void isValid_wrongLength_returnsFalse() {
        assertFalse(AmkaValidator.isValid("0101501234"));   // 10 digits
        assertFalse(AmkaValidator.isValid("010150123456")); // 12 digits
        assertFalse(AmkaValidator.isValid(""));
    }

    @Test
    void isValid_nonDigits_returnsFalse() {
        assertFalse(AmkaValidator.isValid("0101501234A"));
        assertFalse(AmkaValidator.isValid("0101-501234"));
    }

    // ---- validateOrThrow ----

    @Test
    void validateOrThrow_validAmka_doesNotThrow() {
        assertDoesNotThrow(() -> AmkaValidator.validateOrThrow(VALID_1950));
    }

    @Test
    void validateOrThrow_null_throws() {
        var ex = assertThrows(IllegalArgumentException.class,
                () -> AmkaValidator.validateOrThrow(null));
        assertTrue(ex.getMessage().contains("cannot be null"));
    }

    @Test
    void validateOrThrow_wrongLength_reportsLengthFound() {
        var ex = assertThrows(IllegalArgumentException.class,
                () -> AmkaValidator.validateOrThrow("0101501234"));
        assertTrue(ex.getMessage().contains("found 10"));
    }

    @Test
    void validateOrThrow_nonDigits_throws() {
        var ex = assertThrows(IllegalArgumentException.class,
                () -> AmkaValidator.validateOrThrow("0101501234A"));
        assertTrue(ex.getMessage().contains("only digits"));
    }

    @Test
    void validateOrThrow_wrongCheckDigit_throws() {
        var ex = assertThrows(IllegalArgumentException.class,
                () -> AmkaValidator.validateOrThrow("01015012346"));
        assertTrue(ex.getMessage().contains("Luhn"));
    }

    // ---- extractBirthDate ----

    @Test
    void extractBirthDate_1900sYear_returnsNineteenHundreds() {
        assertArrayEquals(new int[]{1, 1, 1950}, AmkaValidator.extractBirthDate(VALID_1950));
        assertArrayEquals(new int[]{12, 12, 1999}, AmkaValidator.extractBirthDate(VALID_1999));
    }

    @Test
    void extractBirthDate_2000sYear_returnsTwoThousands() {
        assertArrayEquals(new int[]{15, 3, 2005}, AmkaValidator.extractBirthDate(VALID_2005));
        assertArrayEquals(new int[]{29, 8, 2000}, AmkaValidator.extractBirthDate(VALID_2000));
    }

    @Test
    void extractBirthDate_impossibleDate_returnsNull() {
        // Luhn-valid, but 31 February does not exist
        assertNull(AmkaValidator.extractBirthDate("31025012340"));
    }

    @Test
    void extractBirthDate_invalidLuhn_returnsNull() {
        assertNull(AmkaValidator.extractBirthDate("01015012346"));
    }
}
