package com.epam.rd.autocode.assessment.appliances.security.password;

import com.epam.rd.autocode.assessment.appliances.mail.SecurityNotificationMail;
import com.epam.rd.autocode.assessment.appliances.repository.password.PasswordResetRepository;
import com.epam.rd.autocode.assessment.appliances.repository.security.LoginAttemptRepository;
import com.epam.rd.autocode.assessment.appliances.repository.security.RefreshTokenRepository;
import com.epam.rd.autocode.assessment.appliances.security.jwt.access.AccessTokenVersionService;

import lombok.RequiredArgsConstructor;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

// Completes a successful password replacement in the existing, account-locked transaction.
@Component
@RequiredArgsConstructor
public class PasswordReplacementEffects {
    private final PasswordCredentialStatusService passwordState;
    private final AccessTokenVersionService tokenVersions;
    private final RefreshTokenRepository refresh;
    private final PasswordResetRepository resetTokens;
    private final LoginAttemptRepository attempts;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    @Transactional(propagation = Propagation.MANDATORY)
    public void complete(Long accountId, String email) {
        passwordState.markSet(accountId);
        tokenVersions.revokePrevious(accountId);
        refresh.revokeAll(accountId, clock.instant());
        resetTokens.deleteAll(accountId);
        attempts.findLocked(email).ifPresent(attempts::delete);
        events.publishEvent(new PasswordChanged(accountId));
        events.publishEvent(
                new SecurityNotificationMail(
                        email,
                        SecurityNotificationMail.Kind.PASSWORD_CHANGED,
                        LocaleContextHolder.getLocale()));
    }
}
