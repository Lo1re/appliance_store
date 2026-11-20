package com.epam.rd.autocode.assessment.appliances.integration;

import com.epam.rd.autocode.assessment.appliances.model.LoginAttempt;
import com.epam.rd.autocode.assessment.appliances.repository.LoginAttemptRepository;
import com.epam.rd.autocode.assessment.appliances.security.login.LoginAttemptService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:login-protection;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(LoginProtectionIntegrationTest.TimeConfiguration.class)
class LoginProtectionIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired LoginAttemptService attempts;
    @SpyBean LoginAttemptRepository repository;
    @Autowired MutableClock clock;
    @Autowired EntityManager entityManager;
    @Autowired PlatformTransactionManager transactions;

    @BeforeEach void prepare() { repository.deleteAll(); clock.set(Instant.parse("2026-01-01T00:00:00Z")); }

    @Test
    void knownAndUnknownAccountsUseTheSameLockPolicyAndExpiry() throws Exception {
        for (String email : new String[]{"mercury@gmail.com", "unknown@example.com"}) {
            for (int i = 0; i < 5; i++) mvc.perform(formLogin().user(email).password("wrong"))
                    .andExpect(unauthenticated()).andExpect(redirectedUrl("/login?error"));
            mvc.perform(formLogin().user("  " + email.toUpperCase() + "  ").password("MercuryLocal2026!"))
                    .andExpect(unauthenticated()).andExpect(redirectedUrl("/login?locked"));
            assertThat(repository.findById(email).orElseThrow().getFailedCount()).isEqualTo(5);
        }
        // Padding beyond the DB email length must not bypass the canonical lock key.
        mvc.perform(formLogin().user(" ".repeat(260) + "mercury@gmail.com").password("MercuryLocal2026!"))
                .andExpect(unauthenticated()).andExpect(redirectedUrl("/login?locked"));
        mvc.perform(get("/login").param("locked", "").param("lang", "uk"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Вхід за паролем тимчасово недоступний.")));
        clock.advance(Duration.ofMinutes(15));
        mvc.perform(formLogin().user("mercury@gmail.com").password("MercuryLocal2026!"))
                .andExpect(authenticated());
        assertThat(repository.existsById("mercury@gmail.com")).isFalse();
    }

    @Test
    void successResetsFailuresAndCounterWritesSurviveAnOuterRollback() throws Exception {
        mvc.perform(formLogin().user("mercury@gmail.com").password("wrong"));
        assertThat(repository.findById("mercury@gmail.com").orElseThrow().getFailedCount()).isEqualTo(1);
        mvc.perform(formLogin().user("mercury@gmail.com").password("MercuryLocal2026!")).andExpect(authenticated());
        assertThat(repository.existsById("mercury@gmail.com")).isFalse();
        new TransactionTemplate(transactions).executeWithoutResult(status -> {
            attempts.failed("rollback@example.com"); status.setRollbackOnly();
        });
        assertThat(repository.findById("rollback@example.com").orElseThrow().getFailedCount()).isEqualTo(1);
        clock.advance(Duration.ofDays(2));
        assertThat(attempts.cleanup()).isEqualTo(1);
        assertThat(repository.existsById("rollback@example.com")).isFalse();
    }

    @Test
    void thirdLoginExpiresTheOldestSessionAndChangesTheSessionId() throws Exception {
        var first = login();
        var second = login();
        var third = login();
        mvc.perform(get("/orders").session(first)).andExpect(redirectedUrl("/login?expired"));
        mvc.perform(get("/orders").session(second)).andExpect(status().isOk());
        mvc.perform(get("/orders").session(third)).andExpect(status().isOk());
        var anonymous = new MockHttpSession(); String oldId = anonymous.getId();
        var authenticated = (MockHttpSession) mvc.perform(post("/login").with(csrf()).session(anonymous)
                        .param("username", "phobos@gmail.com").param("password", "PhobosLocal2026!")).andExpect(authenticated())
                .andReturn().getRequest().getSession(false);
        assertThat(authenticated.getId()).isNotEqualTo(oldId);
    }

    @Test
    void concurrentCreationDoesNotLoseAFailedAttempt() throws Exception {
        var barrier = new CyclicBarrier(2); var reads = new AtomicInteger();
        doAnswer(invocation -> {
            var found = entityManager.find(LoginAttempt.class, "new-counter@example.com", LockModeType.PESSIMISTIC_WRITE);
            if (found == null && reads.incrementAndGet() <= 2) barrier.await(10, TimeUnit.SECONDS);
            return Optional.ofNullable(found);
        }).when(repository).findLocked("new-counter@example.com");
        var executor = Executors.newFixedThreadPool(2);
        try {
            var first = executor.submit(() -> attempts.failed("new-counter@example.com"));
            var second = executor.submit(() -> attempts.failed("new-counter@example.com"));
            first.get(20, TimeUnit.SECONDS); second.get(20, TimeUnit.SECONDS);
        } finally { executor.shutdownNow(); }
        assertThat(repository.findById("new-counter@example.com").orElseThrow().getFailedCount()).isEqualTo(2);
    }

    private MockHttpSession login() throws Exception {
        var session = (MockHttpSession) mvc.perform(formLogin().user("mercury@gmail.com").password("MercuryLocal2026!"))
                .andExpect(authenticated()).andReturn().getRequest().getSession(false);
        Thread.sleep(5); // SessionRegistry uses millisecond timestamps to select the oldest session.
        return session;
    }

    @TestConfiguration static class TimeConfiguration {
        @Bean @Primary MutableClock testClock() { return new MutableClock(); }
    }
    static class MutableClock extends Clock {
        private final AtomicReference<Instant> now = new AtomicReference<>(Instant.parse("2026-01-01T00:00:00Z"));
        void set(Instant value) { now.set(value); }
        void advance(Duration value) { now.updateAndGet(time -> time.plus(value)); }
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return Clock.fixed(instant(), zone); }
        @Override public Instant instant() { return now.get(); }
    }
}
