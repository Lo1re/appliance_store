package com.epam.rd.autocode.assessment.appliances.validation.normalization;

import java.util.Locale;

public final class EmailAddress {
    private EmailAddress() {}

    public static String normalize(String email) {
        return email == null ? null : email.strip().toLowerCase(Locale.ROOT);
    }
}
