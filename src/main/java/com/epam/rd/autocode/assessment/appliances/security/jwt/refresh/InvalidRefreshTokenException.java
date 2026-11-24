package com.epam.rd.autocode.assessment.appliances.security.jwt.refresh;

import org.springframework.security.core.AuthenticationException;

// A deliberately generic error: never carries the submitted credential.
public final class InvalidRefreshTokenException extends AuthenticationException {
    public InvalidRefreshTokenException() {
        super("Invalid refresh credentials");
    }
}
