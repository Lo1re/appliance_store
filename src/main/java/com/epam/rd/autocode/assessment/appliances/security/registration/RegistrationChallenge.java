package com.epam.rd.autocode.assessment.appliances.security.registration;

import java.net.URI;
import java.time.Instant;

public record RegistrationChallenge(
        RegistrationDetails details, String tokenHash, URI link, Instant expiresAt) {
    @Override
    public String toString() {
        return "RegistrationChallenge[redacted]";
    }
}
