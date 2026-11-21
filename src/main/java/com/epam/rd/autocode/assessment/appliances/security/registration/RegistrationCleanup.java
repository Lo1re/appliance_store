package com.epam.rd.autocode.assessment.appliances.security.registration;

import com.epam.rd.autocode.assessment.appliances.repository.registration.PendingRegistrationRepository;
import java.time.Clock;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component @RequiredArgsConstructor @ConditionalOnClientRegistration
public class RegistrationCleanup {
    private final PendingRegistrationRepository pending;
    private final Clock clock;

    @Scheduled(fixedDelayString = "${store.registration.cleanup-interval-ms:3600000}")
    @Transactional
    public void removeExpired() { pending.deleteExpired(clock.instant()); }
}
