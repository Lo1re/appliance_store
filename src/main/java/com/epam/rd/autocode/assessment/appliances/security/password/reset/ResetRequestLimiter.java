package com.epam.rd.autocode.assessment.appliances.security.password.reset;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Bounded single-instance request limits; identifiers are stored only as digests. */
@Component
public class ResetRequestLimiter {
    private static final int CAPACITY = 10000;
    private final Clock clock;
    private final ResetTokenCodec codec;
    private final int maxEmail;
    private final int maxSource;
    private final long windowSeconds;
    private final long cooldownSeconds;

    public ResetRequestLimiter(Clock clock, ResetTokenCodec codec,
            @Value("${store.security.password-reset.max-email-requests:5}") int maxEmail,
            @Value("${store.security.password-reset.max-source-requests:20}") int maxSource,
            @Value("${store.security.password-reset.window-seconds:3600}") long windowSeconds,
            @Value("${store.security.password-reset.cooldown-seconds:60}") long cooldownSeconds) {
        if (maxEmail < 1 || maxSource < 1 || windowSeconds < 1 || cooldownSeconds < 0)
            throw new IllegalArgumentException("Password recovery limits must be positive");
        this.clock = clock; this.codec = codec; this.maxEmail = maxEmail;
        this.maxSource = maxSource; this.windowSeconds = windowSeconds; this.cooldownSeconds = cooldownSeconds;
    }

    private final Map<String, Window> windows = new HashMap<>();

    /** Returns zero when accepted, otherwise the remaining wait in whole seconds (rounded up). */
    public synchronized long acquire(String email, String source) {
        Instant now = clock.instant();
        windows.values().removeIf(window -> !window.until.isAfter(now));
        String emailKey = "email:" + codec.hash(email), sourceKey = "source:" + codec.hash(source);
        int needed = (windows.containsKey(emailKey) ? 0 : 1) + (windows.containsKey(sourceKey) ? 0 : 1);
        if (windows.size() + needed > CAPACITY) {
            return waitSeconds(now, windows.values().stream().map(window -> window.until).min(Instant::compareTo).orElse(now.plusSeconds(1)));
        }
        Window address = windows.computeIfAbsent(emailKey, key -> new Window(now.plusSeconds(windowSeconds)));
        Window origin = windows.computeIfAbsent(sourceKey, key -> new Window(now.plusSeconds(windowSeconds)));
        // Identical gates for existing and unknown addresses; no account lookup here.
        Instant retryAt = now;
        if (origin.count >= maxSource) retryAt = origin.until;
        if (address.count >= maxEmail && address.until.isAfter(retryAt)) retryAt = address.until;
        if (address.nextAllowed.isAfter(retryAt)) retryAt = address.nextAllowed;
        if (retryAt.isAfter(now)) return waitSeconds(now, retryAt);
        // Reserve hourly quota only for requests that pass every gate.
        origin.count++;
        address.count++;
        address.nextAllowed = now.plusSeconds(cooldownSeconds);
        return 0;
    }

    private long waitSeconds(Instant now, Instant until) {
        Duration remaining = Duration.between(now, until);
        return Math.max(1, remaining.getSeconds() + (remaining.getNano() == 0 ? 0 : 1));
    }

    private static class Window {
        private final Instant until;
        private long count;
        private Instant nextAllowed = Instant.MIN;
        private Window(Instant until) { this.until = until; }
    }
}
