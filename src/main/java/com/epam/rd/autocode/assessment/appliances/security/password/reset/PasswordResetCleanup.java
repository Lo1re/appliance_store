package com.epam.rd.autocode.assessment.appliances.security.password.reset;

import com.epam.rd.autocode.assessment.appliances.repository.password.PasswordResetRepository;
import java.time.Clock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component @RequiredArgsConstructor @Slf4j
public class PasswordResetCleanup {
    private final PasswordResetRepository tokens;
    private final Clock clock;
    @Scheduled(fixedDelayString = "${store.security.password-reset.cleanup-interval-ms:3600000}")
    @Transactional
    public void clean() {
        int removed = tokens.deleteExpired(clock.instant());
        if (removed > 0) log.debug("Expired password reset records removed (count={})", removed);
    }
}
