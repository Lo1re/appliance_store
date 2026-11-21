package com.epam.rd.autocode.assessment.appliances.security.registration;

import com.epam.rd.autocode.assessment.appliances.model.registration.PendingRegistration;
import com.epam.rd.autocode.assessment.appliances.persistence.PersistenceBoundary;
import com.epam.rd.autocode.assessment.appliances.persistence.PersistenceOperation;
import com.epam.rd.autocode.assessment.appliances.repository.registration.PendingRegistrationRepository;
import com.epam.rd.autocode.assessment.appliances.security.password.reset.ResetTokenCodec;
import java.time.Clock;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Single-use confirmation; locking and client creation share one transaction. */
@Component @RequiredArgsConstructor @ConditionalOnClientRegistration
public class RegistrationConfirmation {
    private final PendingRegistrationRepository pending;
    private final ResetTokenCodec tokens;
    private final ClientRegistrationService clients;
    private final Clock clock;

    @Transactional(readOnly = true)
    public RegistrationDetails preview(String token) {
        return details(requireLive(pending.findById(digest(token)).orElseThrow(InvalidRegistrationException::new)));
    }

    @Transactional
    @PersistenceBoundary(PersistenceOperation.ACCOUNT_WRITE)
    public Long complete(String token) {
        var registration = requireLive(pending.lockByHash(digest(token)).orElseThrow(InvalidRegistrationException::new));
        Long id = clients.register(details(registration));
        pending.delete(registration);
        return id;
    }

    private RegistrationDetails details(PendingRegistration registration) {
        return new RegistrationDetails(registration.getName(), registration.getEmail(), registration.getCard(), registration.getPasswordHash());
    }
    private String digest(String token) {
        if (!tokens.valid(token)) throw new InvalidRegistrationException();
        return tokens.hash(token);
    }
    private PendingRegistration requireLive(PendingRegistration registration) {
        if (!registration.getExpiresAt().isAfter(clock.instant())) throw new InvalidRegistrationException();
        return registration;
    }
}
