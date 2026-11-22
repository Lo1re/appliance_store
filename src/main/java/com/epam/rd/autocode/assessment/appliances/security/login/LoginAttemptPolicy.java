package com.epam.rd.autocode.assessment.appliances.security.login;

import com.epam.rd.autocode.assessment.appliances.model.LoginAttempt;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Time-window rules only; persistence and authentication are handled elsewhere. */
@Component
@RequiredArgsConstructor
public class LoginAttemptPolicy {
    private final LoginProtectionProperties properties;

    public boolean locked(LoginAttempt attempt, Instant now) {
        return attempt.getLockedUntil() != null && now.isBefore(attempt.getLockedUntil());
    }

    public LoginAttempt failed(String email, LoginAttempt current, Instant now) {
        if (current != null && locked(current, now)) {
            return current;
        }
        if (current == null) {
            current = new LoginAttempt(email, 0, now, null);
        } else if (current.getLockedUntil() != null
                || !now.isBefore(current.getFirstFailedAt().plus(properties.window()))) {
            current.setFailedCount(0); current.setFirstFailedAt(now); current.setLockedUntil(null);
        }
        current.setFailedCount(Math.min(properties.maxFailures(), current.getFailedCount() + 1));
        if (current.getFailedCount() >= properties.maxFailures()) {
            current.setLockedUntil(now.plus(properties.lockDuration()));
        }
        return current;
    }
}
