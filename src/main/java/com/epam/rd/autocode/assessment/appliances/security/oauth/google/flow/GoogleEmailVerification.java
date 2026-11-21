package com.epam.rd.autocode.assessment.appliances.security.oauth.google.flow;

import com.epam.rd.autocode.assessment.appliances.mail.EmailVerificationMail;
import com.epam.rd.autocode.assessment.appliances.mail.SecurityMailSender;
import com.epam.rd.autocode.assessment.appliances.security.oauth.google.config.ConditionalOnGoogleLogin;
import com.epam.rd.autocode.assessment.appliances.security.oauth.google.config.GoogleLoginProperties;
import com.epam.rd.autocode.assessment.appliances.security.password.reset.ResetRequestLimiter;
import com.epam.rd.autocode.assessment.appliances.security.password.reset.ResetTokenCodec;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.stereotype.Component;

@Component @RequiredArgsConstructor @ConditionalOnGoogleLogin
public class GoogleEmailVerification {
    private final GoogleFlowSession flows;
    private final ResetRequestLimiter limits;
    private final ResetTokenCodec tokens;
    private final GoogleLoginProperties properties;
    private final ObjectProvider<SecurityMailSender> mail;
    private final ApplicationEventPublisher events;

    public long send(HttpServletRequest request, String email, Locale locale) {
        flows.requirePending(request);
        if (mail.getIfAvailable() == null) throw new AuthenticationServiceException("Email verification unavailable");
        long retry = limits.acquire("google-verify:" + email, "google-verify:" + request.getRemoteAddr());
        if (retry > 0) return retry;
        String token = tokens.generate();
        flows.challenge(request, email, tokens.hash(token));
        String base = properties.getRedirectUri().substring(0, properties.getRedirectUri().length() - "/login/oauth2/code/google".length());
        events.publishEvent(new EmailVerificationMail(email, URI.create(base + "/onboarding/google/verify?token=" + token), locale));
        return 0;
    }
    public void confirm(HttpServletRequest request, String token) {
        if (!tokens.valid(token)) throw new com.epam.rd.autocode.assessment.appliances.security.oauth.google.identity.GoogleLoginException(
                com.epam.rd.autocode.assessment.appliances.security.oauth.google.identity.GoogleLoginException.Reason.FLOW_EXPIRED);
        flows.verifyEmail(request, tokens.hash(token));
    }
}
