package com.epam.rd.autocode.assessment.appliances.security.registration;

import com.epam.rd.autocode.assessment.appliances.dto.registration.ClientRegistrationRequest;
import com.epam.rd.autocode.assessment.appliances.mail.RegistrationVerificationMail;
import com.epam.rd.autocode.assessment.appliances.model.registration.PendingRegistration;
import com.epam.rd.autocode.assessment.appliances.repository.registration.PendingRegistrationRepository;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import lombok.RequiredArgsConstructor;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.Locale;

// Persists the hashed signup challenge before scheduling its email after commit.
@Component
@RequiredArgsConstructor
@Validated
@ConditionalOnClientRegistration
public class RegistrationSubmission {
    private final RegistrationPreparation preparation;
    private final PendingRegistrationRepository pending;
    private final ApplicationEventPublisher events;

    @Transactional
    public long submit(
            @NotNull @Valid ClientRegistrationRequest form, String source, Locale locale) {
        var result = preparation.prepare(form, source);
        if (result.retryAfter() > 0) {
            return result.retryAfter();
        }
        var challenge = result.challenge();
        var details = challenge.details();
        pending.saveAndFlush(
                new PendingRegistration(
                        challenge.tokenHash(),
                        details.name(),
                        details.email(),
                        details.card(),
                        details.passwordHash(),
                        challenge.expiresAt()));
        events.publishEvent(
                new RegistrationVerificationMail(
                        challenge.details().email(), challenge.link(), locale));
        return 0;
    }
}
