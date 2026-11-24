package com.epam.rd.autocode.assessment.appliances.security.oauth.google.identity;

public class GoogleLoginException extends RuntimeException {
    private final Reason reason;

    public enum Reason {
        INVALID_IDENTITY,
        LINK_REQUIRED,
        LINK_CONFLICT,
        FLOW_EXPIRED
    }

    public GoogleLoginException(Reason reason) {
        super("Google sign-in could not be completed");
        this.reason = reason;
    }

    public Reason reason() {
        return reason;
    }
}
