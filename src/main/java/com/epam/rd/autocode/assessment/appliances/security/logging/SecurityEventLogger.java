package com.epam.rd.autocode.assessment.appliances.security.logging;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.authentication.event.LogoutSuccessEvent;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SecurityEventLogger {
    @EventListener
    public void authenticated(AuthenticationSuccessEvent event) {
        Authentication authentication = event.getAuthentication();
        log.info("Authentication succeeded (accountId={}, remoteAddress={})",
                SecurityLogContext.accountId(authentication), SecurityLogContext.remoteAddress(authentication));
    }

    @EventListener
    public void authenticationFailed(AbstractAuthenticationFailureEvent event) {
        log.warn("Authentication failed (type={}, remoteAddress={})",
                event.getException().getClass().getSimpleName(),
                SecurityLogContext.remoteAddress(event.getAuthentication()));
    }

    @EventListener
    public void loggedOut(LogoutSuccessEvent event) {
        log.info("Logout succeeded (accountId={})", SecurityLogContext.accountId(event.getAuthentication()));
    }
}
