package com.epam.rd.autocode.assessment.appliances.repository.projection;

/** Internal database projection, never returned by a web endpoint. */
public record CredentialSnapshot(String email, String passwordHash) {
    @Override public String toString() { return "CredentialSnapshot[credentials=REDACTED]"; }
}
