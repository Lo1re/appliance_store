package com.epam.rd.autocode.assessment.appliances.security.login;

import com.epam.rd.autocode.assessment.appliances.repository.LoginAttemptRepository;
import com.epam.rd.autocode.assessment.appliances.validation.EmailAddress;
import java.time.Clock;
import java.time.Duration;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/** Persists counters in independent transactions, including failures of an outer login. */
@Component
public class LoginAttemptService {
    private final LoginAttemptRepository attempts;
    private final LoginAttemptPolicy policy;
    private final LoginProtectionProperties properties;
    private final Clock clock;
    private final TransactionTemplate transaction;

    public LoginAttemptService(LoginAttemptRepository attempts, LoginAttemptPolicy policy,
                               LoginProtectionProperties properties, Clock clock, PlatformTransactionManager manager) {
        this.attempts = attempts; this.policy = policy; this.properties = properties; this.clock = clock;
        transaction = new TransactionTemplate(manager);
        transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public Duration remainingLock(String username) {
        var key = key(username);
        if (key.isEmpty()) return Duration.ZERO;
        var now = clock.instant();
        return attempts.findById(key.get()).filter(attempt -> policy.locked(attempt, now))
                .map(attempt -> Duration.between(now, attempt.getLockedUntil())).orElse(Duration.ZERO);
    }

    public void failed(String username) {
        var key = key(username);
        if (key.isEmpty()) return;
        for (int retry = 0; ; retry++) {
            try {
                transaction.executeWithoutResult(status -> {
                    var current = attempts.findLocked(key.get()).orElse(null);
                    attempts.saveAndFlush(policy.failed(key.get(), current, clock.instant()));
                });
                return;
            } catch (DataIntegrityViolationException concurrentInsert) {
                // The failed transaction has rolled back before checking the competing insert.
                if (retry == 2 || !attempts.existsById(key.get())) throw concurrentInsert;
            }
        }
    }

    public void succeeded(String username) {
        key(username).ifPresent(email -> transaction.executeWithoutResult(status ->
                attempts.findLocked(email).filter(attempt -> !policy.locked(attempt, clock.instant()))
                        .ifPresent(attempts::delete)));
    }

    public int cleanup() {
        var now = clock.instant();
        return transaction.execute(status -> attempts.deleteExpired(now.minus(properties.retention()), now));
    }

    private Optional<String> key(String username) {
        if (username == null || username.isBlank()) return Optional.empty();
        String email = EmailAddress.normalize(username);
        return email.length() > 254 ? Optional.empty() : Optional.of(email);
    }
}
