package com.epam.rd.autocode.assessment.appliances.security.password;

import com.epam.rd.autocode.assessment.appliances.exception.password.PasswordChangeError;
import com.epam.rd.autocode.assessment.appliances.exception.password.PasswordChangeException;
import com.epam.rd.autocode.assessment.appliances.logging.SafeFailure;
import com.epam.rd.autocode.assessment.appliances.security.login.LoginAttemptService;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionException;

/** Current-secret verification respects the password lock but never publishes a login-success event. */
@Component
@RequiredArgsConstructor
@Slf4j
public class CurrentPasswordVerifier {
    private final PasswordEncoder encoder;
    private final LoginAttemptService attempts;
    public void verify(Long accountId, String email, String raw, String encoded) {
        try {
            requireUnlocked(email);
            if (raw == null || raw.getBytes(StandardCharsets.UTF_8).length > 72 || !encoder.matches(raw, encoded)) {
                attempts.failed(email);
                log.warn("Password change rejected (accountId={})", accountId);
                throw new PasswordChangeException(PasswordChangeError.CURRENT_INVALID);
            }
            requireUnlocked(email);
        } catch (DataAccessException | TransactionException failure) {
            throw new AuthenticationServiceException("Authentication temporarily unavailable", SafeFailure.snapshot(failure));
        }
    }
    private void requireUnlocked(String email) {
        if (!attempts.remainingLock(email).isZero()) throw new LockedException("Password verification temporarily unavailable");
    }
}
