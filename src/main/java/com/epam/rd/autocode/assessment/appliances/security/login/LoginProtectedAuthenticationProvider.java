package com.epam.rd.autocode.assessment.appliances.security.login;

import com.epam.rd.autocode.assessment.appliances.logging.SafeFailure;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.transaction.TransactionException;

/** Gates password authentication using fresh lock state, never a principal snapshot. */
@RequiredArgsConstructor
public class LoginProtectedAuthenticationProvider implements AuthenticationProvider {
    private final AuthenticationProvider passwords;
    private final LoginAttemptService attempts;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        try {
            requireUnlocked(authentication.getName());
            if (authentication.getCredentials() instanceof String password
                    && password.getBytes(StandardCharsets.UTF_8).length > 72) {
                throw new BadCredentialsException("Bad credentials");
            }
            Authentication authenticated = passwords.authenticate(authentication);
            requireUnlocked(authentication.getName());
            return authenticated;
        } catch (DataAccessException | TransactionException | AuthenticationServiceException failure) {
            throw new AuthenticationServiceException("Authentication temporarily unavailable", SafeFailure.snapshot(failure));
        }
    }

    private void requireUnlocked(String username) {
        if (!attempts.remainingLock(username).isZero()) {
            throw new LockedException("Password login temporarily unavailable");
        }
    }

    @Override public boolean supports(Class<?> authentication) { return passwords.supports(authentication); }
}
