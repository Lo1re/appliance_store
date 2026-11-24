package com.epam.rd.autocode.assessment.appliances.security.jwt.api;

import lombok.Getter;

import org.springframework.security.authentication.LockedException;

import java.io.Serial;

@Getter
public class PasswordLoginLockedException extends LockedException {
    @Serial private static final long serialVersionUID = 1L;
    private final long retryAfterSeconds;

    public PasswordLoginLockedException(long retryAfterSeconds) {
        super("Password login temporarily unavailable");
        this.retryAfterSeconds = Math.max(1, retryAfterSeconds);
    }
}
