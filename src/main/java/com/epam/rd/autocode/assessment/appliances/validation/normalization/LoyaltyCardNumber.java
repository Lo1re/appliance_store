package com.epam.rd.autocode.assessment.appliances.validation.normalization;

// Optional loyalty-card number: preserve its value, remove surrounding whitespace.
public final class LoyaltyCardNumber {
    private LoyaltyCardNumber() {}

    public static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.strip();
    }
}
