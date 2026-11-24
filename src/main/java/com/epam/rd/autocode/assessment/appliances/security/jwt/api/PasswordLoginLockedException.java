package com.epam.rd.autocode.assessment.appliances.security.jwt.api;

import org.springframework.security.authentication.LockedException;

public class PasswordLoginLockedException extends LockedException {
    private static final long serialVersionUID = 1L;
    private final long retryAfterSeconds;

    public PasswordLoginLockedException(long retryAfterSeconds) {
        super("Password login temporarily unavailable");
        this.retryAfterSeconds = Math.max(1, retryAfterSeconds);
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
