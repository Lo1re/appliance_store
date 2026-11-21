package com.epam.rd.autocode.assessment.appliances.security.registration;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Internal, server-held registration data; never bound from a confirmation request. */
public record RegistrationDetails(@NotBlank @Size(max = 100) String name,
        @NotBlank @Email @Size(max = 254) String email, @Size(max = 32) String card,
        @NotBlank String passwordHash) {
    @Override public String toString() { return "RegistrationDetails[redacted]"; }
}
