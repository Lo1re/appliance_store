package com.epam.rd.autocode.assessment.appliances.security.login;

import com.epam.rd.autocode.assessment.appliances.logging.SafeFailure;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class LoginAttemptCleanup {
    private final LoginAttemptService attempts;

    @Scheduled(
            fixedDelayString = "${store.security.login.cleanup-interval-ms:3600000}",
            initialDelayString = "${store.security.login.cleanup-interval-ms:3600000}")
    public void cleanExpiredCounters() {
        try {
            log.debug("Expired login counters removed (count={})", attempts.cleanup());
        } catch (RuntimeException failure) {
            log.error("Login counter cleanup failed", SafeFailure.snapshot(failure));
        }
    }
}
