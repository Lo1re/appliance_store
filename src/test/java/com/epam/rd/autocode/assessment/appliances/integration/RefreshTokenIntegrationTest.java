package com.epam.rd.autocode.assessment.appliances.integration;

import static org.assertj.core.api.Assertions.*;

import com.epam.rd.autocode.assessment.appliances.model.Client;
import com.epam.rd.autocode.assessment.appliances.model.security.RefreshToken;
import com.epam.rd.autocode.assessment.appliances.repository.ClientRepository;
import com.epam.rd.autocode.assessment.appliances.repository.security.RefreshTokenRepository;
import com.epam.rd.autocode.assessment.appliances.security.jwt.TokenPair;
import com.epam.rd.autocode.assessment.appliances.security.jwt.refresh.InvalidRefreshTokenException;
import com.epam.rd.autocode.assessment.appliances.security.jwt.refresh.RefreshTokenCodec;
import com.epam.rd.autocode.assessment.appliances.security.jwt.refresh.RefreshTokenService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.*;

@SpringBootTest(
        properties = {
            "spring.datasource.url=jdbc:h2:mem:refresh-lifecycle;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
            "store.security.web-auth=jwt",
            "store.api.enabled=false",
            "store.security.jwt.secret-base64=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="
        })
@ActiveProfiles("test")
class RefreshTokenIntegrationTest {
    @Autowired RefreshTokenService refresh;
    @Autowired RefreshTokenCodec codec;
    @Autowired RefreshTokenRepository tokens;
    @Autowired ClientRepository clients;
    @Autowired AuthenticationManager passwords;
    @Autowired PlatformTransactionManager manager;
    @Autowired JdbcTemplate jdbc;

    @Autowired
    com.epam.rd.autocode.assessment.appliances.repository.security.LoginAttemptRepository attempts;

    @BeforeEach
    void prepare() {
        tokens.deleteAll();
        attempts.deleteAll();
    }

    @Test
    void rotationStoresOnlyHashesAndReuseRevocationCommitsEvenWhenCallerRollsBack() {
        var first = issue();
        var stored = tokens.findByTokenHash(codec.hash(first.refreshToken())).orElseThrow();
        assertThat(stored.getTokenHash()).hasSize(64).isNotEqualTo(first.refreshToken());
        assertThat(stored.getExpiresAt()).isAfter(Instant.now().plusSeconds(6 * 86400));
        assertThat(first.toString())
                .doesNotContain(first.refreshToken(), first.access().accessToken());
        var second = refresh.rotate(first.refreshToken());
        var original = tokens.findById(stored.getId()).orElseThrow();
        assertThat(original.getRevokedAt()).isNotNull();
        assertThat(original.getReplacedBy()).isNotNull();
        assertThat(second.refreshToken()).isNotEqualTo(first.refreshToken());
        new TransactionTemplate(manager)
                .executeWithoutResult(
                        status -> {
                            assertThatThrownBy(() -> refresh.rotate(first.refreshToken()))
                                    .isInstanceOf(InvalidRefreshTokenException.class);
                            status.setRollbackOnly();
                        });
        assertThat(
                        tokens.findByTokenHash(codec.hash(second.refreshToken()))
                                .orElseThrow()
                                .getRevokedAt())
                .isNotNull();
        assertThatThrownBy(() -> refresh.rotate(second.refreshToken()))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void concurrentRefreshCannotIssueTwoUsableSuccessors() throws Exception {
        var first = issue();
        var pool = Executors.newFixedThreadPool(2);
        var start = new CountDownLatch(1);
        try {
            Callable<Boolean> attempt =
                    () -> {
                        start.await();
                        try {
                            refresh.rotate(first.refreshToken());
                            return true;
                        } catch (InvalidRefreshTokenException rejected) {
                            return false;
                        }
                    };
            var one = pool.submit(attempt);
            var two = pool.submit(attempt);
            start.countDown();
            assertThat(List.of(one.get(30, TimeUnit.SECONDS), two.get(30, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(true, false);
            // The losing reuse revokes the winner's continuation too, according to the reuse
            // policy.
            assertThat(tokens.findAll()).allMatch(token -> token.getRevokedAt() != null);
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void logoutIsIdempotentAndDoesNotRevokeAnotherBrowser() {
        var one = issue();
        var two = issue();
        refresh.revoke(one.refreshToken());
        refresh.revoke(one.refreshToken());
        refresh.revoke(null);
        assertThat(
                        tokens.findByTokenHash(codec.hash(one.refreshToken()))
                                .orElseThrow()
                                .getRevokedAt())
                .isNotNull();
        assertThat(
                        tokens.findByTokenHash(codec.hash(two.refreshToken()))
                                .orElseThrow()
                                .getRevokedAt())
                .isNull();
        assertThat(refresh.rotate(two.refreshToken())).isNotNull();
    }

    @Test
    void expiryRetentionAndAccountCascadeKeepOnlyRequiredSecurityRecords() {
        var first = issue();
        var stored = tokens.findByTokenHash(codec.hash(first.refreshToken())).orElseThrow();
        jdbc.update(
                "update refresh_token set expires_at = ? where id = ?",
                java.sql.Timestamp.from(Instant.now().minusSeconds(1)),
                stored.getId());
        assertThatThrownBy(() -> refresh.rotate(first.refreshToken()))
                .isInstanceOf(InvalidRefreshTokenException.class);
        assertThat(refresh.cleanup()).isZero();
        jdbc.update(
                "update refresh_token set expires_at = ? where id = ?",
                java.sql.Timestamp.from(Instant.now().minusSeconds(9 * 86400)),
                stored.getId());
        assertThat(refresh.cleanup()).isEqualTo(1);
        assertThat(tokens.findById(stored.getId())).isEmpty();
        var client =
                clients.saveAndFlush(
                        new Client(
                                null,
                                "Refresh cascade",
                                "refresh-cascade@example.com",
                                clients.findAll().get(0).getPassword(),
                                "REFRESH-CASCADE"));
        var raw = codec.generate();
        tokens.saveAndFlush(
                new RefreshToken(client.getId(), codec.hash(raw), Instant.now().plusSeconds(3600)));
        clients.deleteById(client.getId());
        clients.flush();
        assertThat(tokens.findByTokenHash(codec.hash(raw))).isEmpty();
        assertThatThrownBy(() -> refresh.rotate(raw))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void refreshingExistingAccessDoesNotResetThePasswordLockCounter() {
        var initial = issue();
        for (int i = 0; i < 5; i++) {
            assertThatThrownBy(
                            () ->
                                    passwords.authenticate(
                                            UsernamePasswordAuthenticationToken.unauthenticated(
                                                    "mercury@gmail.com", "wrong")))
                    .isInstanceOf(org.springframework.security.core.AuthenticationException.class);
        }
        assertThat(attempts.findById("mercury@gmail.com").orElseThrow().getFailedCount())
                .isEqualTo(5);
        assertThat(refresh.rotate(initial.refreshToken())).isNotNull();
        assertThat(attempts.findById("mercury@gmail.com").orElseThrow().getFailedCount())
                .isEqualTo(5);
    }

    @Test
    void logoutWithAnOldRotatedTokenRevokesItsSuccessorsButNotAnotherBrowser() {
        var first = issue();
        var otherBrowser = issue();
        var second = refresh.rotate(first.refreshToken());
        var third = refresh.rotate(second.refreshToken());
        // Rotation won the serialization lock before logout. Old-cookie logout must still close
        // that continuation.
        refresh.revoke(first.refreshToken());
        refresh.revoke(first.refreshToken());
        assertThat(
                        tokens.findByTokenHash(codec.hash(third.refreshToken()))
                                .orElseThrow()
                                .getRevokedAt())
                .isNotNull();
        assertThat(
                        tokens.findByTokenHash(codec.hash(otherBrowser.refreshToken()))
                                .orElseThrow()
                                .getRevokedAt())
                .isNull();
        assertThat(refresh.rotate(otherBrowser.refreshToken())).isNotNull();
    }

    @Test
    void passwordReloginRetiresThePreviousBrowserChainAtomically() {
        var previous = issue();
        var sibling = refresh.rotate(previous.refreshToken());
        var otherBrowser = issue();
        var authentication =
                passwords.authenticate(
                        UsernamePasswordAuthenticationToken.unauthenticated(
                                "mercury@gmail.com", "MercuryLocal2026!"));
        var replacement = refresh.issue(authentication, previous.refreshToken());
        assertThat(
                        tokens.findByTokenHash(codec.hash(sibling.refreshToken()))
                                .orElseThrow()
                                .getRevokedAt())
                .isNotNull();
        assertThat(
                        tokens.findByTokenHash(codec.hash(replacement.refreshToken()))
                                .orElseThrow()
                                .getRevokedAt())
                .isNull();
        assertThat(
                        tokens.findByTokenHash(codec.hash(otherBrowser.refreshToken()))
                                .orElseThrow()
                                .getRevokedAt())
                .isNull();
    }

    @Test
    void concurrentAccountSwitchesUseAStableOwnerLockOrder() throws Exception {
        var mercury =
                passwords.authenticate(
                        UsernamePasswordAuthenticationToken.unauthenticated(
                                "mercury@gmail.com", "MercuryLocal2026!"));
        var venus =
                passwords.authenticate(
                        UsernamePasswordAuthenticationToken.unauthenticated(
                                "venus@gmail.com", "VenusLocal2026!"));
        var mercuryBrowser = refresh.issue(mercury);
        var venusBrowser = refresh.issue(venus);
        var pool = Executors.newFixedThreadPool(2);
        var start = new CountDownLatch(1);
        try {
            var one =
                    pool.submit(
                            () -> {
                                start.await();
                                return refresh.issue(mercury, venusBrowser.refreshToken());
                            });
            var two =
                    pool.submit(
                            () -> {
                                start.await();
                                return refresh.issue(venus, mercuryBrowser.refreshToken());
                            });
            start.countDown();
            var first = one.get(30, TimeUnit.SECONDS);
            var second = two.get(30, TimeUnit.SECONDS);
            assertThat(
                            tokens.findByTokenHash(codec.hash(first.refreshToken()))
                                    .orElseThrow()
                                    .getRevokedAt())
                    .isNull();
            assertThat(
                            tokens.findByTokenHash(codec.hash(second.refreshToken()))
                                    .orElseThrow()
                                    .getRevokedAt())
                    .isNull();
            assertThat(
                            tokens.findByTokenHash(codec.hash(mercuryBrowser.refreshToken()))
                                    .orElseThrow()
                                    .getRevokedAt())
                    .isNotNull();
            assertThat(
                            tokens.findByTokenHash(codec.hash(venusBrowser.refreshToken()))
                                    .orElseThrow()
                                    .getRevokedAt())
                    .isNotNull();
        } finally {
            pool.shutdownNow();
        }
    }

    private TokenPair issue() {
        return refresh.issue(
                passwords.authenticate(
                        UsernamePasswordAuthenticationToken.unauthenticated(
                                "mercury@gmail.com", "MercuryLocal2026!")));
    }
}
