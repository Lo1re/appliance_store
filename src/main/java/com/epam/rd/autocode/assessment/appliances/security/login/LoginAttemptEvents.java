package com.epam.rd.autocode.assessment.appliances.security.login;

import com.epam.rd.autocode.assessment.appliances.logging.SafeFailure;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;

/** Counts only password events, exactly once; OAuth and locked attempts are excluded. */
@Slf4j
@Component
@RequiredArgsConstructor
public class LoginAttemptEvents {
    private final LoginAttemptService attempts;

    @EventListener
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public void failed(AuthenticationFailureBadCredentialsEvent event) {
        if (event.getAuthentication() instanceof UsernamePasswordAuthenticationToken) {
            try { attempts.failed(event.getAuthentication().getName()); }
            catch (RuntimeException failure) {
                log.error("Login counter persistence failed", SafeFailure.snapshot(failure));
                throw new AuthenticationServiceException("Authentication temporarily unavailable", SafeFailure.snapshot(failure));
            }
        }
    }

    @EventListener
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public void succeeded(AuthenticationSuccessEvent event) {
        if (event.getAuthentication() instanceof UsernamePasswordAuthenticationToken) {
            try { attempts.succeeded(event.getAuthentication().getName()); }
            catch (RuntimeException failure) {
                log.error("Login counter reset failed", SafeFailure.snapshot(failure));
                throw new AuthenticationServiceException("Authentication temporarily unavailable", SafeFailure.snapshot(failure));
            }
        }
    }
}
