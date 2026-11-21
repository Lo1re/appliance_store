package com.epam.rd.autocode.assessment.appliances.security.jwt.refresh;

import com.epam.rd.autocode.assessment.appliances.logging.SafeFailure;
import com.epam.rd.autocode.assessment.appliances.security.jwt.config.ConditionalOnJwt;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnJwt
@RequiredArgsConstructor
public class RefreshTokenCleanup {
    private final RefreshTokenService tokens;

    @Scheduled(fixedDelayString = "${store.security.refresh.cleanup-interval-ms:3600000}",
            initialDelayString = "${store.security.refresh.cleanup-interval-ms:3600000}")
    public void cleanup() {
        try { log.debug("Refresh retention cleanup completed (removedCount={})", tokens.cleanup()); }
        catch (RuntimeException failure) { log.error("Refresh retention cleanup failed", SafeFailure.snapshot(failure)); }
    }
}
