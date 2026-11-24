package com.epam.rd.autocode.assessment.appliances.security.oauth.google.identity;

// Constructed only after Spring's OIDC verification and our claim checks; never MVC-bound.
public record VerifiedGoogleIdentity(
        String subject, String email, String name, boolean emailVerified) {
    @Override
    public String toString() {
        return "VerifiedGoogleIdentity[redacted]";
    }
}
