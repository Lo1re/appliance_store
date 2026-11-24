package com.epam.rd.autocode.assessment.appliances.security.logging;

import static org.assertj.core.api.Assertions.*;

import com.epam.rd.autocode.assessment.appliances.security.identity.StorePrincipal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.authentication.event.LogoutSuccessEvent;
import org.springframework.security.web.authentication.WebAuthenticationDetails;

@ExtendWith(OutputCaptureExtension.class)
class SecurityEventLoggerTest {
    @Test
    void recordsSafeIdentityAndRemoteAddressWithoutCredentialsOrForwardedHeader(
            CapturedOutput output) {
        var principal =
                new StorePrincipal(
                        7L, "private-email@example.com", "private-password-hash", "EMPLOYEE");
        var authentication =
                UsernamePasswordAuthenticationToken.authenticated(
                        principal, "private-credentials", principal.getAuthorities());
        var request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");
        request.addHeader("X-Forwarded-For", "untrusted-forwarded-address");
        authentication.setDetails(new WebAuthenticationDetails(request));
        var logger = new SecurityEventLogger();
        logger.authenticated(new AuthenticationSuccessEvent(authentication));
        logger.authenticationFailed(
                new AuthenticationFailureBadCredentialsEvent(
                        authentication, new BadCredentialsException("private-failure-message")));
        logger.loggedOut(new LogoutSuccessEvent(authentication));
        assertThat(output.getOut())
                .contains(
                        "accountId=7",
                        "remoteAddress=127.0.0.1",
                        "Authentication failed",
                        "Logout succeeded")
                .doesNotContain(
                        "private-email@example.com",
                        "private-password-hash",
                        "private-credentials",
                        "private-failure-message",
                        "untrusted-forwarded-address");
    }
}
