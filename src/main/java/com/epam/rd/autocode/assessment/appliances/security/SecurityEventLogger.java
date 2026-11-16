package com.epam.rd.autocode.assessment.appliances.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.authentication.event.LogoutSuccessEvent;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SecurityEventLogger {
    @EventListener
    public void authenticated(AuthenticationSuccessEvent event) {
        log.info("Authentication succeeded");
    }

    @EventListener
    public void authenticationFailed(AbstractAuthenticationFailureEvent event) {
        log.warn("Authentication failed (type={})", event.getException().getClass().getSimpleName());
    }

    @EventListener
    public void loggedOut(LogoutSuccessEvent event) {
        log.info("Logout succeeded");
    }
}
