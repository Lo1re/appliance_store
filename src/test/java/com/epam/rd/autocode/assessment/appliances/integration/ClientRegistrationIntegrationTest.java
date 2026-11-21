package com.epam.rd.autocode.assessment.appliances.integration;

import com.epam.rd.autocode.assessment.appliances.mail.SecurityMailSender;
import com.epam.rd.autocode.assessment.appliances.repository.ClientRepository;
import com.epam.rd.autocode.assessment.appliances.repository.UserRepository;
import com.epam.rd.autocode.assessment.appliances.security.jwt.cookie.AuthTokenCookies;
import java.net.URI;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:client-registration;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
    "store.registration.enabled=true", "store.mail.enabled=true", "store.security.web-auth=jwt",
    "store.security.jwt.secret-base64=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="})
@AutoConfigureMockMvc @ActiveProfiles({"local", "test"})
class ClientRegistrationIntegrationTest {
    private static final String PASSWORD = "RegistrationPass2026!";
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired ClientRepository clients;
    @Autowired PasswordEncoder encoder;
    @Autowired com.epam.rd.autocode.assessment.appliances.repository.registration.PendingRegistrationRepository pending;
    @Autowired com.epam.rd.autocode.assessment.appliances.security.registration.RegistrationConfirmation confirmations;
    @Autowired com.epam.rd.autocode.assessment.appliances.security.registration.RegistrationCleanup cleanup;
    @Autowired org.springframework.transaction.PlatformTransactionManager transactions;
    @Autowired org.springframework.jdbc.core.JdbcTemplate jdbc;
    @Autowired com.epam.rd.autocode.assessment.appliances.security.registration.RegistrationSubmission submissions;
    @MockBean SecurityMailSender mail;
    @Autowired @Qualifier("securityMailExecutor") ThreadPoolTaskExecutor executor;
    @AfterEach void drainMail() throws Exception { executor.submit(() -> {}).get(3, TimeUnit.SECONDS); }

    @Test void emailVerificationCreatesOnlyClientAndThenAllowsPasswordLogin() throws Exception {
        String email = email();
        mvc.perform(get("/register")).andExpect(status().isOk()).andExpect(view().name("registration/form"));
        var submitted = mvc.perform(form(email).with(browserCsrf())).andExpect(redirectedUrl("/register/check-email")).andReturn();
        assertThat(submitted.getRequest().getSession(false)).isNull();
        assertThat(users.existsByEmailIgnoreCase(email)).isFalse();
        var link = ArgumentCaptor.forClass(URI.class);
        verify(mail, timeout(3000)).sendRegistrationVerification(eq(email), link.capture(), any());
        String token = link.getValue().getRawQuery().substring("token=".length());
        var page = mvc.perform(get("/register/verify").param("token", token)).andExpect(status().isOk())
                .andExpect(view().name("registration/confirm")).andReturn();
        assertThat(page.getRequest().getSession(false)).isNull();
        assertThat(users.existsByEmailIgnoreCase(email)).isFalse();
        mvc.perform(get("/register/verify").param("token", token)).andExpect(status().isOk());
        mvc.perform(post("/register/complete").param("token", token)).andExpect(status().isForbidden());
        var csrfToken = (org.springframework.security.web.csrf.CsrfToken) page.getRequest().getAttribute(org.springframework.security.web.csrf.CsrfToken.class.getName());
        mvc.perform(post("/register/complete").param("token", token)
                .cookie(page.getResponse().getCookie("STORE-CSRF")).param(csrfToken.getParameterName(), csrfToken.getToken())
                .param("role", "EMPLOYEE").param("email", "forged@example.test").param("password", "forged"))
                .andExpect(redirectedUrl("/login?registered"));
        var client = clients.findAll().stream().filter(c -> email.equals(c.getEmail())).findFirst().orElseThrow();
        assertThat(encoder.matches(PASSWORD, client.getPassword())).isTrue();
        assertThat(users.existsByEmailIgnoreCase("forged@example.test")).isFalse();
        verify(mail, timeout(3000)).sendAccountCreated(eq(email), any());
        mvc.perform(get("/register/verify").param("token", token)).andExpect(redirectedUrl("/register?invalid"));
        mvc.perform(post("/register/complete").param("token", token).with(browserCsrf())).andExpect(redirectedUrl("/register?invalid"));
        mvc.perform(post("/register/complete").with(browserCsrf())).andExpect(redirectedUrl("/register?invalid"));
        var login = mvc.perform(post("/login").with(browserCsrf()).param("username", email).param("password", PASSWORD)).andReturn();
        assertThat(login.getResponse().getCookie(AuthTokenCookies.NAME)).isNotNull();
        mvc.perform(get("/employees").cookie(login.getResponse().getCookie(AuthTokenCookies.NAME)))
                .andExpect(status().isForbidden());
    }
    @Test void validationAndCsrfFailuresNeverCreateAccountsOrEchoPasswords() throws Exception {
        String email = email();
        mvc.perform(form(email)).andExpect(status().isForbidden());
        var invalid = mvc.perform(post("/register").with(browserCsrf()).param("name", "New Client").param("email", email).param("card", "REG-CARD").param("newPassword", PASSWORD).param("confirmPassword", "DifferentPassword2026!"))
                .andExpect(status().isBadRequest()).andReturn();
        assertThat(invalid.getResponse().getContentAsString()).doesNotContain(PASSWORD, "DifferentPassword2026!");
        assertThat(users.existsByEmailIgnoreCase(email)).isFalse();
        verifyNoInteractions(mail);
    }
    @Test void cooldownRejectsRetryWithoutSendingAnotherMessage() throws Exception {
        String email = email();
        mvc.perform(form(email).with(browserCsrf())).andExpect(redirectedUrl("/register/check-email"));
        mvc.perform(form(email).with(browserCsrf())).andExpect(status().isTooManyRequests()).andExpect(header().exists("Retry-After"));
        executor.submit(() -> {}).get(3, TimeUnit.SECONDS);
        verify(mail, times(1)).sendRegistrationVerification(eq(email), any(), any());
        assertThat(users.existsByEmailIgnoreCase(email)).isFalse();
    }
    @Test void duplicateEmailAndCardReturnFieldErrorsWithoutSendingMail() throws Exception {
        String existing = email();
        String card = "CARD-" + UUID.randomUUID().toString().substring(0, 20);
        clients.saveAndFlush(new com.epam.rd.autocode.assessment.appliances.model.Client(null, "Existing", existing, encoder.encode(PASSWORD), card));
        mvc.perform(form(existing).with(browserCsrf())).andExpect(status().isConflict())
                .andExpect(model().attributeHasFieldErrorCode("registrationForm", "email", "user.email.duplicate"));
        String fresh = email();
        mvc.perform(post("/register").with(browserCsrf()).param("name", "Client").param("email", fresh).param("card", card)
                .param("newPassword", PASSWORD).param("confirmPassword", PASSWORD))
                .andExpect(status().isConflict()).andExpect(model().attributeHasFieldErrorCode("registrationForm", "card", "client.card.duplicate"));
        verifyNoInteractions(mail);
        assertThat(users.existsByEmailIgnoreCase(fresh)).isFalse();
    }
    @Test void registrationAllowsOmittedCard() throws Exception {
        String fresh = email();
        mvc.perform(post("/register").with(browserCsrf()).param("name", "Client").param("email", fresh)
                .param("newPassword", PASSWORD).param("confirmPassword", PASSWORD))
                .andExpect(redirectedUrl("/register/check-email"));
        verify(mail, timeout(3000)).sendRegistrationVerification(eq(fresh), any(), any());
    }
    @Test void expiredLinkCannotCreateAccountAndCleanupRemovesOnlyExpiredRows() throws Exception {
        String token = submitAndToken(email());
        String hash = new com.epam.rd.autocode.assessment.appliances.security.password.reset.ResetTokenCodec().hash(token);
        jdbc.update("update pending_registration set expires_at = ? where token_hash = ?", java.time.OffsetDateTime.now().minusMinutes(1), hash);
        mvc.perform(get("/register/verify").param("token", token)).andExpect(redirectedUrl("/register?invalid"));
        mvc.perform(post("/register/complete").param("token", token).with(browserCsrf())).andExpect(redirectedUrl("/register?invalid"));
        String live = submitAndToken(email());
        cleanup.removeExpired();
        assertThat(pending.findById(hash)).isEmpty();
        assertThat(confirmations.preview(live)).isNotNull();
    }
    @Test void confirmationRollbackPreservesApplicationAndDoesNotSendWelcome() throws Exception {
        String address = email(); String token = submitAndToken(address);
        new org.springframework.transaction.support.TransactionTemplate(transactions).executeWithoutResult(status -> {
            confirmations.complete(token); status.setRollbackOnly();
        });
        assertThat(users.existsByEmailIgnoreCase(address)).isFalse();
        assertThat(confirmations.preview(token).email()).isEqualTo(address);
        executor.submit(() -> {}).get(3, TimeUnit.SECONDS);
        verify(mail, never()).sendAccountCreated(eq(address), any());
        confirmations.complete(token);
        verify(mail, timeout(3000)).sendAccountCreated(eq(address), any());
    }
    @Test void concurrentConfirmationsCreateExactlyOneClient() throws Exception {
        String address = email(); String token = submitAndToken(address);
        var workers = java.util.concurrent.Executors.newFixedThreadPool(2);
        var ready = new java.util.concurrent.CyclicBarrier(2);
        java.util.concurrent.Callable<Boolean> confirm = () -> {
            ready.await(5, TimeUnit.SECONDS);
            try { confirmations.complete(token); return true; }
            catch (com.epam.rd.autocode.assessment.appliances.security.registration.InvalidRegistrationException used) { return false; }
        };
        try {
            var a = workers.submit(confirm); var b = workers.submit(confirm);
            assertThat(java.util.List.of(a.get(10, TimeUnit.SECONDS), b.get(10, TimeUnit.SECONDS))).containsExactlyInAnyOrder(true, false);
            assertThat(clients.findAll().stream().filter(c -> address.equals(c.getEmail())).count()).isEqualTo(1);
        } finally { workers.shutdownNow(); }
    }
    @Test void rolledBackIssuanceDoesNotStoreApplicationOrSendEmail() throws Exception {
        String address = email();
        var form = new com.epam.rd.autocode.assessment.appliances.dto.registration.ClientRegistrationRequest();
        form.setName("Rollback"); form.setEmail(address); form.setNewPassword(PASSWORD); form.setConfirmPassword(PASSWORD);
        new org.springframework.transaction.support.TransactionTemplate(transactions).executeWithoutResult(status -> {
            assertThat(submissions.submit(form, "rollback-source", java.util.Locale.ENGLISH)).isZero();
            status.setRollbackOnly();
        });
        assertThat(pending.findAll().stream().anyMatch(row -> address.equals(row.getEmail()))).isFalse();
        executor.submit(() -> {}).get(3, TimeUnit.SECONDS);
        verify(mail, never()).sendRegistrationVerification(eq(address), any(), any());
    }
    private String submitAndToken(String address) throws Exception {
        mvc.perform(form(address).with(browserCsrf())).andExpect(redirectedUrl("/register/check-email"));
        var link = ArgumentCaptor.forClass(URI.class);
        verify(mail, timeout(3000)).sendRegistrationVerification(eq(address), link.capture(), any());
        return link.getValue().getRawQuery().substring("token=".length());
    }
    /** Exercise the actual cookie repository; csrf() replaces it with a session repository in Spring Security Test 6.2. */
    private org.springframework.test.web.servlet.request.RequestPostProcessor browserCsrf() throws Exception {
        var page = mvc.perform(get("/register")).andExpect(status().isOk()).andReturn();
        var token = (org.springframework.security.web.csrf.CsrfToken) page.getRequest()
                .getAttribute(org.springframework.security.web.csrf.CsrfToken.class.getName());
        var cookie = page.getResponse().getCookie("STORE-CSRF");
        assertThat(page.getRequest().getSession(false)).isNull();
        assertThat(token).isNotNull(); assertThat(cookie).isNotNull();
        return request -> {
            var cookies = new java.util.ArrayList<jakarta.servlet.http.Cookie>();
            if (request.getCookies() != null) {
                for (var existing : request.getCookies()) if (!cookie.getName().equals(existing.getName())) cookies.add(existing);
            }
            cookies.add(cookie); request.setCookies(cookies.toArray(jakarta.servlet.http.Cookie[]::new));
            request.setParameter(token.getParameterName(), token.getToken());
            return request;
        };
    }
    private static String email() { return "register-" + UUID.randomUUID() + "@example.test"; }
    private static MockHttpServletRequestBuilder form(String email) {
        return post("/register").param("name", "New Client").param("email", email).param("card", "REG-" + Integer.toUnsignedString(email.hashCode()))
                .param("newPassword", PASSWORD).param("confirmPassword", PASSWORD);
    }
}
