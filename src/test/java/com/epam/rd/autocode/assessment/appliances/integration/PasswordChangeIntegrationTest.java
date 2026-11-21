package com.epam.rd.autocode.assessment.appliances.integration;

import com.epam.rd.autocode.assessment.appliances.dto.client.ClientRequest;
import com.epam.rd.autocode.assessment.appliances.dto.password.PasswordChangeRequest;
import com.epam.rd.autocode.assessment.appliances.exception.password.PasswordChangeException;
import com.epam.rd.autocode.assessment.appliances.model.Client;
import com.epam.rd.autocode.assessment.appliances.repository.ClientRepository;
import com.epam.rd.autocode.assessment.appliances.repository.LoginAttemptRepository;
import com.epam.rd.autocode.assessment.appliances.repository.RefreshTokenRepository;
import com.epam.rd.autocode.assessment.appliances.repository.UserRepository;
import com.epam.rd.autocode.assessment.appliances.security.identity.AccountRole;
import com.epam.rd.autocode.assessment.appliances.security.identity.StorePrincipal;
import com.epam.rd.autocode.assessment.appliances.security.jwt.refresh.RefreshTokenCodec;
import com.epam.rd.autocode.assessment.appliances.security.jwt.refresh.RefreshTokenService;
import com.epam.rd.autocode.assessment.appliances.security.login.LoginAttemptService;
import com.epam.rd.autocode.assessment.appliances.service.ClientService;
import com.epam.rd.autocode.assessment.appliances.service.password.PasswordChangeService;
import jakarta.validation.ConstraintViolationException;
import java.util.UUID;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(properties = {"spring.datasource.url=jdbc:h2:mem:password-change;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "store.security.web-auth=jwt", "store.api.enabled=false",
        "store.security.jwt.secret-base64=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="})
@ActiveProfiles("test")
class PasswordChangeIntegrationTest {
    private static final String OLD = "OldReviewPass2026!", NEW = "NewReviewPass2026!";
    @Autowired ClientRepository clients; @Autowired UserRepository users; @Autowired RefreshTokenRepository tokens;
    @Autowired RefreshTokenService refresh; @Autowired RefreshTokenCodec codec; @Autowired PasswordEncoder encoder;
    @Autowired PasswordChangeService passwords; @Autowired ClientService clientService;
    @Autowired LoginAttemptService attempts; @Autowired LoginAttemptRepository counters;
    @Autowired com.epam.rd.autocode.assessment.appliances.security.jwt.access.AccessTokenVersionService tokenVersions;
    @Autowired SessionRegistry sessions; @Autowired PlatformTransactionManager manager;
    private Client client; private Authentication authentication;
    @BeforeEach void prepare() {
        client = clients.saveAndFlush(new Client(null, "Password review", "password-"+UUID.randomUUID()+"@example.com", encoder.encode(OLD), "PW-REVIEW"));
        var principal = new StorePrincipal(client.getId(), client.getEmail(), client.getPassword(), AccountRole.CLIENT);
        authentication = UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
    @AfterEach void clear() { SecurityContextHolder.clearContext(); }

    @Test void commitsHashRefreshRevocationCounterResetAndOnlyTargetSessionExpiration() {
        var pair = refresh.issue(authentication); attempts.failed(client.getEmail());
        String target="pw-target-"+UUID.randomUUID(), other="pw-other-"+UUID.randomUUID();
        sessions.registerNewSession(target, authentication.getPrincipal());
        sessions.registerNewSession(other, new StorePrincipal(999999L,"other@example.com","hash",AccountRole.CLIENT));
        passwords.change(form(OLD,NEW));
        String encoded=users.findById(client.getId()).orElseThrow().getPassword();
        assertThat(encoded).startsWith("$2a$").isNotEqualTo(NEW); assertThat(encoder.matches(NEW,encoded)).isTrue();
        assertThat(encoder.matches(OLD,encoded)).isFalse();
        assertThat(tokens.findByTokenHash(codec.hash(pair.refreshToken())).orElseThrow().getRevokedAt()).isNotNull();
        assertThat(counters.findById(client.getEmail())).isEmpty();
        assertThat(tokenVersions.current(client.getId())).isEqualTo(1);
        assertThat(sessions.getSessionInformation(target).isExpired()).isTrue();
        assertThat(sessions.getSessionInformation(other).isExpired()).isFalse();
    }

    @Test void outerRollbackPreservesHashTokensCounterAndSessions() {
        var pair=refresh.issue(authentication); attempts.failed(client.getEmail());
        String session="pw-rollback-"+UUID.randomUUID();sessions.registerNewSession(session,authentication.getPrincipal());
        new TransactionTemplate(manager).executeWithoutResult(status -> {passwords.change(form(OLD,NEW));status.setRollbackOnly();});
        assertThat(encoder.matches(OLD,users.findById(client.getId()).orElseThrow().getPassword())).isTrue();
        assertThat(tokens.findByTokenHash(codec.hash(pair.refreshToken())).orElseThrow().getRevokedAt()).isNull();
        assertThat(counters.findById(client.getEmail()).orElseThrow().getFailedCount()).isEqualTo(1);
        assertThat(tokenVersions.current(client.getId())).isZero();
        assertThat(sessions.getSessionInformation(session).isExpired()).isFalse();
    }

    @Test void wrongCurrentPasswordPersistsFailureButDoesNotChangeCredentialsOrTokens() {
        var pair=refresh.issue(authentication);
        assertThatThrownBy(() -> passwords.change(form("wrong",NEW))).isInstanceOf(PasswordChangeException.class).hasMessage("password.current.invalid");
        assertThat(counters.findById(client.getEmail()).orElseThrow().getFailedCount()).isEqualTo(1);
        assertThat(encoder.matches(OLD,users.findById(client.getId()).orElseThrow().getPassword())).isTrue();
        assertThat(tokens.findByTokenHash(codec.hash(pair.refreshToken())).orElseThrow().getRevokedAt()).isNull();
    }

    @Test void validationAndUnchangedPasswordCannotRevokeExistingAccess() {
        var pair=refresh.issue(authentication);var mismatch=form(OLD,NEW);mismatch.setConfirmPassword("DifferentReviewPass2026!");
        assertThatThrownBy(() -> passwords.change(mismatch)).isInstanceOf(ConstraintViolationException.class);
        assertThatThrownBy(() -> passwords.change(form(OLD,"weak"))).isInstanceOf(ConstraintViolationException.class);
        assertThatThrownBy(() -> passwords.change(form(OLD,OLD))).isInstanceOf(PasswordChangeException.class).hasMessage("password.new.unchanged");
        assertThat(tokens.findByTokenHash(codec.hash(pair.refreshToken())).orElseThrow().getRevokedAt()).isNull();
        assertThat(counters.findById(client.getEmail())).isEmpty();
    }

    @Test void authenticatedPasswordChangeCannotBypassTheSharedPasswordLock() {
        for(int i=0;i<5;i++)assertThatThrownBy(() -> passwords.change(form("wrong",NEW))).isInstanceOf(PasswordChangeException.class);
        assertThatThrownBy(() -> passwords.change(form(OLD,NEW))).isInstanceOf(LockedException.class);
        assertThat(encoder.matches(OLD,users.findById(client.getId()).orElseThrow().getPassword())).isTrue();
    }

    @Test void staleProfileEntityInAnOuterTransactionCannotRestoreTheOldPassword() throws Exception {
        var employee=users.findByEmailIgnoreCase("phobos@gmail.com").orElseThrow();
        var principal=new StorePrincipal(employee.getId(),employee.getEmail(),employee.getPassword(),AccountRole.EMPLOYEE);
        var employeeAuth=UsernamePasswordAuthenticationToken.authenticated(principal,null,principal.getAuthorities());
        var loaded=new CountDownLatch(1);var passwordCommitted=new CountDownLatch(1);var pool=Executors.newSingleThreadExecutor();
        try {
            var update=pool.submit(() -> {
                SecurityContextHolder.getContext().setAuthentication(employeeAuth);
                try {new TransactionTemplate(manager).executeWithoutResult(status -> {
                    clients.findById(client.getId()).orElseThrow();loaded.countDown();
                    try {if(!passwordCommitted.await(20,TimeUnit.SECONDS))throw new IllegalStateException("Password commit timed out");}
                    catch(InterruptedException interrupted){Thread.currentThread().interrupt();throw new IllegalStateException("Interrupted");}
                    var profile=new ClientRequest();profile.setName("Updated profile");profile.setEmail(client.getEmail());profile.setCard("UPDATED");
                    clientService.update(client.getId(),profile);
                });} finally {SecurityContextHolder.clearContext();}
            });
            assertThat(loaded.await(10,TimeUnit.SECONDS)).isTrue();passwords.change(form(OLD,NEW));passwordCommitted.countDown();update.get(30,TimeUnit.SECONDS);
            var saved=users.findById(client.getId()).orElseThrow();
            assertThat(saved.getName()).isEqualTo("Updated profile");assertThat(encoder.matches(NEW,saved.getPassword())).isTrue();
        } finally {passwordCommitted.countDown();pool.shutdownNow();}
    }

    private PasswordChangeRequest form(String current,String next) {
        var request=new PasswordChangeRequest();request.setCurrentPassword(current);request.setNewPassword(next);request.setConfirmPassword(next);return request;
    }
}
