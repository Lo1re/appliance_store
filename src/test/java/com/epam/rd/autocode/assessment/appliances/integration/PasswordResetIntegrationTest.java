package com.epam.rd.autocode.assessment.appliances.integration;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.epam.rd.autocode.assessment.appliances.dto.password.PasswordResetRequest;
import com.epam.rd.autocode.assessment.appliances.mail.SecurityMailSender;
import com.epam.rd.autocode.assessment.appliances.model.Client;
import com.epam.rd.autocode.assessment.appliances.model.security.RefreshToken;
import com.epam.rd.autocode.assessment.appliances.repository.ClientRepository;
import com.epam.rd.autocode.assessment.appliances.repository.UserRepository;
import com.epam.rd.autocode.assessment.appliances.repository.security.RefreshTokenRepository;
import com.epam.rd.autocode.assessment.appliances.security.password.reset.InvalidPasswordResetException;
import com.epam.rd.autocode.assessment.appliances.security.password.reset.PasswordResetService;
import com.epam.rd.autocode.assessment.appliances.security.password.reset.ResetTokenCodec;

import jakarta.validation.ConstraintViolationException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.net.URI;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

@ActiveProfiles({"local", "test"})
@AutoConfigureMockMvc
@SpringBootTest(
        properties = {
            "spring.datasource.url=jdbc:h2:mem:password-reset-it;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
            "store.security.password-reset.cooldown-seconds=0",
            "store.mail.enabled=true",
            "store.mail.base-url=http://localhost:8080",
            "store.security.web-auth=session"
        })
class PasswordResetIntegrationTest {
    private static final String OLD = "BeforeReset2026!";
    private static final String NEW = "AfterReset2026!";
    @Autowired PasswordResetService passwords;
    @Autowired ClientRepository clients;
    @Autowired UserRepository users;
    @Autowired RefreshTokenRepository refresh;
    @Autowired PasswordEncoder encoder;
    @Autowired ResetTokenCodec codec;
    @Autowired JdbcTemplate jdbc;
    @Autowired PlatformTransactionManager transactions;
    @Autowired MockMvc mvc;
    @Autowired org.springframework.security.core.session.SessionRegistry sessions;
    @MockBean SecurityMailSender mail;

    @Autowired
    @org.springframework.beans.factory.annotation.Qualifier("securityMailExecutor")
    org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor mailExecutor;

    private Client client;

    @org.junit.jupiter.api.AfterEach
    void drainSecurityMailQueue() throws Exception {
        mailExecutor.submit(() -> {}).get(3, java.util.concurrent.TimeUnit.SECONDS);
    }

    @BeforeEach
    void prepare() {
        reset(mail);
        client =
                clients.save(
                        new Client(
                                null,
                                "Reset test",
                                "reset-" + UUID.randomUUID() + "@example.test",
                                encoder.encode(OLD),
                                null));
    }

    @Test
    void sendsOnlyAfterCommitAndStoresOnlyHashThenConsumesOnce() {
        String token = requestToken();
        assertThat(
                        jdbc.queryForObject(
                                "select token_hash from password_reset_token where user_id = ?",
                                String.class,
                                client.getId()))
                .isEqualTo(codec.hash(token))
                .isNotEqualTo(token);
        var session =
                refresh.save(
                        new RefreshToken(
                                client.getId(),
                                codec.hash("refresh-test-" + UUID.randomUUID()),
                                Instant.now().plusSeconds(3600)));
        String sessionId = "reset-session-" + UUID.randomUUID();
        sessions.registerNewSession(
                sessionId,
                new com.epam.rd.autocode.assessment.appliances.security.identity.StorePrincipal(
                        client.getId(),
                        client.getEmail(),
                        "redacted",
                        com.epam.rd.autocode.assessment.appliances.security.identity.AccountRole
                                .CLIENT));
        jdbc.update(
                "insert into login_attempt(email, failed_count, first_failed_at, locked_until)"
                        + " values (?, 5, ?, ?)",
                client.getEmail(),
                java.sql.Timestamp.from(Instant.now()),
                java.sql.Timestamp.from(Instant.now().plusSeconds(900)));
        passwords.reset(form(token));
        assertThat(sessions.getSessionInformation(sessionId).isExpired()).isTrue();
        assertThat(
                        jdbc.queryForObject(
                                "select count(*) from login_attempt where email = ?",
                                Long.class,
                                client.getEmail()))
                .isZero();
        verify(mail, timeout(3000)).sendPasswordChanged(eq(client.getEmail()), any(Locale.class));
        String hash = users.findById(client.getId()).orElseThrow().getPassword();
        assertThat(encoder.matches(NEW, hash)).isTrue();
        assertThat(encoder.matches(OLD, hash)).isFalse();
        assertThat(refresh.findById(session.getId()).orElseThrow().getRevokedAt()).isNotNull();
        assertThatThrownBy(() -> passwords.reset(form(token)))
                .isInstanceOf(InvalidPasswordResetException.class);
    }

    @Test
    void issuanceRollbackDoesNotSendAndResetRollbackPreservesTokenAndPassword() {
        var tx = new TransactionTemplate(transactions);
        tx.executeWithoutResult(
                status -> {
                    passwords.request(client.getEmail(), "issue-rollback", Locale.ENGLISH);
                    status.setRollbackOnly();
                });
        verifyNoInteractions(mail);
        String token = requestToken();
        tx.executeWithoutResult(
                status -> {
                    passwords.reset(form(token));
                    status.setRollbackOnly();
                });
        assertThat(encoder.matches(OLD, users.findById(client.getId()).orElseThrow().getPassword()))
                .isTrue();
        verify(mail, never()).sendPasswordChanged(anyString(), any(Locale.class));
        passwords.reset(form(token));
    }

    @Test
    void replacementAndExpiryInvalidateLinksWithoutChangingPassword() {
        String first = requestToken();
        String second = requestToken();
        assertThatThrownBy(() -> passwords.reset(form(first)))
                .isInstanceOf(InvalidPasswordResetException.class);
        jdbc.update(
                "update password_reset_token set expires_at = ? where user_id = ?",
                java.sql.Timestamp.from(Instant.now().minusSeconds(1)),
                client.getId());
        assertThatThrownBy(() -> passwords.reset(form(second)))
                .isInstanceOf(InvalidPasswordResetException.class);
        assertThat(encoder.matches(OLD, users.findById(client.getId()).orElseThrow().getPassword()))
                .isTrue();
    }

    @Test
    void weakPasswordDoesNotConsumeValidLink() {
        String token = requestToken();
        var form = form(token);
        form.setNewPassword("weak");
        form.setConfirmPassword("weak");
        assertThatThrownBy(() -> passwords.reset(form))
                .isInstanceOf(ConstraintViolationException.class);
        passwords.reset(form(token));
    }

    @Test
    void unknownEmailAndKnownEmailHaveSameResponseAndCsrfRemainsRequired() throws Exception {
        mvc.perform(post("/password/forgot").param("email", client.getEmail()))
                .andExpect(status().isForbidden());
        for (String email :
                new String[] {
                    client.getEmail(), "unknown-" + UUID.randomUUID() + "@example.test"
                }) {
            mvc.perform(post("/password/forgot").with(csrf()).param("email", email))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/password/forgot?sent"));
        }
        verify(mail, timeout(3000).times(1)).sendPasswordReset(eq(client.getEmail()), any(), any());
        mvc.perform(get("/password/reset").param("token", "invalid"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(header().string("Referrer-Policy", "no-referrer"));
        mvc.perform(
                        post("/password/reset")
                                .param("token", "invalid")
                                .param("newPassword", NEW)
                                .param("confirmPassword", NEW))
                .andExpect(status().isForbidden());
    }

    @Test
    void fullAnonymousHttpResetDoesNotEchoSecrets() throws Exception {
        String token = requestToken();
        mvc.perform(
                        post("/password/reset")
                                .with(csrf())
                                .param("token", token)
                                .param("newPassword", "weak")
                                .param("confirmPassword", "weak"))
                .andExpect(status().isBadRequest())
                .andExpect(
                        content()
                                .string(
                                        org.hamcrest.Matchers.not(
                                                org.hamcrest.Matchers.containsString(
                                                        "value=\"weak\""))));
        mvc.perform(
                        post("/password/reset")
                                .with(csrf())
                                .param("token", token)
                                .param("newPassword", NEW)
                                .param("confirmPassword", NEW))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?passwordChanged"));
        mvc.perform(
                        post("/password/reset")
                                .with(csrf())
                                .param("token", token)
                                .param("newPassword", NEW)
                                .param("confirmPassword", NEW))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unchangedPasswordIsRejectedWithoutConsumingTheLink() throws Exception {
        String token = requestToken();
        mvc.perform(
                        post("/password/reset")
                                .with(csrf())
                                .param("token", token)
                                .param("newPassword", OLD)
                                .param("confirmPassword", OLD))
                .andExpect(status().isBadRequest())
                .andExpect(model().attributeHasFieldErrors("resetForm", "newPassword"));
        passwords.reset(form(token));
    }

    @Test
    void repeatedRequestsShowRemainingSecondsForKnownAndUnknownEmails() throws Exception {
        // This test context disables the short cooldown for link-replacement scenarios;
        // exhaust its real hourly per-email limit to exercise the same HTTP 429 path.
        for (String email :
                new String[] {
                    client.getEmail(), "unknown-" + UUID.randomUUID() + "@example.test"
                }) {
            String source = "limit-" + UUID.randomUUID();
            for (int i = 0; i < 5; i++) {
                passwords.request(email, source, Locale.ENGLISH);
            }
            mvc.perform(
                            post("/password/forgot")
                                    .with(csrf())
                                    .with(
                                            request -> {
                                                request.setRemoteAddr(source);
                                                return request;
                                            })
                                    .param("email", email)
                                    .cookie(new jakarta.servlet.http.Cookie("store-lang", "uk")))
                    .andExpect(status().isTooManyRequests())
                    .andExpect(header().exists("Retry-After"))
                    .andExpect(model().attributeExists("retryAfterSeconds"))
                    .andExpect(
                            content().string(org.hamcrest.Matchers.containsString("Зачекайте ще")));
        }
    }

    @Test
    void concurrentRedemptionHasExactlyOneWinner() throws Exception {
        String token = requestToken();
        var pool = java.util.concurrent.Executors.newFixedThreadPool(2);
        var start = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.Callable<Boolean> redeem =
                () -> {
                    start.await();
                    try {
                        passwords.reset(form(token));
                        return true;
                    } catch (InvalidPasswordResetException invalid) {
                        return false;
                    }
                };
        try {
            var first = pool.submit(redeem);
            var second = pool.submit(redeem);
            start.countDown();
            assertThat(
                            java.util.List.of(
                                    first.get(20, java.util.concurrent.TimeUnit.SECONDS),
                                    second.get(20, java.util.concurrent.TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(true, false);
        } finally {
            start.countDown();
            pool.shutdownNow();
        }
    }

    private String requestToken() {
        clearInvocations(mail);
        passwords.request(client.getEmail(), "source-" + client.getId(), Locale.ENGLISH);
        var link = ArgumentCaptor.forClass(URI.class);
        verify(mail, timeout(3000))
                .sendPasswordReset(eq(client.getEmail()), link.capture(), eq(Locale.ENGLISH));
        return link.getValue().getRawQuery().substring("token=".length());
    }

    private PasswordResetRequest form(String token) {
        var form = new PasswordResetRequest();
        form.setToken(token);
        form.setNewPassword(NEW);
        form.setConfirmPassword(NEW);
        return form;
    }
}
