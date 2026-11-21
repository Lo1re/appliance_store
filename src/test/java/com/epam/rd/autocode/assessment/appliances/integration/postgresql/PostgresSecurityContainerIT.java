package com.epam.rd.autocode.assessment.appliances.integration.postgresql;

import com.epam.rd.autocode.assessment.appliances.model.Client;
import com.epam.rd.autocode.assessment.appliances.repository.ClientRepository;
import com.epam.rd.autocode.assessment.appliances.repository.UserRepository;
import com.epam.rd.autocode.assessment.appliances.repository.RefreshTokenRepository;
import com.epam.rd.autocode.assessment.appliances.repository.password.PasswordResetRepository;
import com.epam.rd.autocode.assessment.appliances.security.identity.StorePrincipal;
import com.epam.rd.autocode.assessment.appliances.security.jwt.refresh.RefreshTokenService;
import com.epam.rd.autocode.assessment.appliances.security.jwt.refresh.RefreshTokenCodec;
import com.epam.rd.autocode.assessment.appliances.security.jwt.refresh.InvalidRefreshTokenException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import static org.assertj.core.api.Assertions.*;

class PostgresSecurityContainerIT extends PostgresContainerSupport {
    @Autowired RefreshTokenService refresh;
    @Autowired RefreshTokenCodec codec;
    @Autowired RefreshTokenRepository tokens;
    @Autowired PasswordResetRepository resets;
    @Autowired UserRepository users;
    @Autowired ClientRepository clients;
    @Autowired PlatformTransactionManager manager;

    @Test void parallelRefreshHasOneWinnerAndReplayRevokesItsSuccessor() throws Exception {
        var client = client(); var principal = new StorePrincipal(client.getId(), client.getEmail(), client.getPassword(), "CLIENT");
        var auth = UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities());
        var first = refresh.issue(auth); var pool = Executors.newFixedThreadPool(2); var start = new CyclicBarrier(2);
        Callable<Boolean> rotate = () -> { start.await(5, TimeUnit.SECONDS); try { refresh.rotate(first.refreshToken()); return true; }
            catch (InvalidRefreshTokenException rejected) { return false; } };
        try {
            var a = pool.submit(rotate); var b = pool.submit(rotate);
            assertThat(List.of(a.get(30, TimeUnit.SECONDS), b.get(30, TimeUnit.SECONDS))).containsExactlyInAnyOrder(true, false);
            assertThat(tokens.findAll().stream().filter(token -> client.getId().equals(token.getUserId())).toList())
                    .isNotEmpty().allMatch(token -> token.getRevokedAt() != null);
        } finally { pool.shutdownNow(); }
    }
    @Test void passwordAndConsumedResetTokenRollbackTogetherOnPostgres() {
        var client = client(); String hash = codec.hash(codec.generate());
        var transaction = new TransactionTemplate(manager);
        transaction.executeWithoutResult(status -> resets.replace(client.getId(), hash, Instant.now().plusSeconds(1800)));
        transaction.executeWithoutResult(status -> {
            assertThat(users.lockIdentity(client.getId())).contains(client.getId());
            assertThat(resets.consume(client.getId(), hash, Instant.now())).isTrue();
            assertThat(users.replacePassword(client.getId(), client.getPassword(), "rolled-back-password-hash")).isEqualTo(1);
            status.setRollbackOnly();
        });
        assertThat(resets.owner(hash)).contains(client.getId());
        assertThat(users.findById(client.getId()).orElseThrow().getPassword()).isEqualTo(client.getPassword());
        transaction.executeWithoutResult(status -> {
            assertThat(resets.consume(client.getId(), hash, Instant.now())).isTrue();
            assertThat(resets.consume(client.getId(), hash, Instant.now())).isFalse();
        });
    }
    private Client client() {
        String password = users.findByEmailIgnoreCase("mercury@gmail.com").orElseThrow().getPassword();
        return clients.saveAndFlush(new Client(null, "Container client", "security-" + UUID.randomUUID() + "@example.test", password, null));
    }
}
