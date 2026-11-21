package com.epam.rd.autocode.assessment.appliances.integration;

import com.epam.rd.autocode.assessment.appliances.mail.SecurityMailSender;
import com.epam.rd.autocode.assessment.appliances.model.Client;
import com.epam.rd.autocode.assessment.appliances.repository.ClientRepository;
import com.epam.rd.autocode.assessment.appliances.repository.UserRepository;
import com.epam.rd.autocode.assessment.appliances.repository.oauth.OAuthIdentityRepository;
import com.epam.rd.autocode.assessment.appliances.security.jwt.cookie.AuthTokenCookies;
import com.epam.rd.autocode.assessment.appliances.security.jwt.cookie.JwtCookieLifecycle;
import com.epam.rd.autocode.assessment.appliances.security.jwt.refresh.RefreshTokenService;
import com.epam.rd.autocode.assessment.appliances.security.oauth.google.flow.GoogleFlowSession;
import com.epam.rd.autocode.assessment.appliances.security.oauth.google.flow.GoogleLinkVerifier;
import com.epam.rd.autocode.assessment.appliances.security.oauth.google.identity.GoogleAccountAuthentication;
import com.epam.rd.autocode.assessment.appliances.security.oauth.google.identity.GoogleIdentityService;
import com.epam.rd.autocode.assessment.appliances.security.oauth.google.identity.GoogleIdentityVerifier;
import com.epam.rd.autocode.assessment.appliances.security.oauth.google.identity.GoogleLoginException;
import com.epam.rd.autocode.assessment.appliances.security.oauth.google.identity.VerifiedGoogleIdentity;
import com.epam.rd.autocode.assessment.appliances.security.oauth.google.web.GoogleLoginSuccessHandler;
import com.epam.rd.autocode.assessment.appliances.security.password.reset.ResetTokenCodec;
import jakarta.servlet.http.Cookie;
import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles({"local", "test"}) @AutoConfigureMockMvc
@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:google-login-it;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
    "store.security.web-auth=jwt", "store.security.google.enabled=true",
    "store.security.google.client-id=test-google-client", "store.security.google.client-secret=test-only-not-a-real-secret",
    "store.security.google.redirect-uri=http://localhost:8080/login/oauth2/code/google",
    "store.security.jwt.secret-base64=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=", "store.mail.enabled=true"
})
class GoogleLoginIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired GoogleFlowSession flows;
    @Autowired com.epam.rd.autocode.assessment.appliances.security.password.PasswordCredentialStatusService passwordState;
    @Autowired com.epam.rd.autocode.assessment.appliances.security.password.reset.PasswordResetService resetPasswords;
    @Autowired org.springframework.transaction.PlatformTransactionManager transactions;
    @Autowired GoogleIdentityService identities;
    @Autowired GoogleIdentityVerifier verifier;
    @Autowired GoogleAccountAuthentication accounts;
    @Autowired RefreshTokenService tokens;
    @Autowired AuthTokenCookies browserCookies;
    @Autowired JwtCookieLifecycle cookies;
    @Autowired ResetTokenCodec hashes;
    @Autowired JwtDecoder decoder;
    @Autowired ClientRepository clients;
    @Autowired UserRepository users;
    @Autowired OAuthIdentityRepository links;
    @Autowired PasswordEncoder encoder;
    @MockBean SecurityMailSender mail;
    @Autowired @org.springframework.beans.factory.annotation.Qualifier("securityMailExecutor")
    org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor mailExecutor;
    @org.junit.jupiter.api.AfterEach void drainSecurityMailQueue() throws Exception {
        mailExecutor.submit(() -> { }).get(3, java.util.concurrent.TimeUnit.SECONDS);
    }

    private String subject;
    private String email;

    @BeforeEach void prepare() {
        reset(mail); subject = "subject-" + UUID.randomUUID(); email = "google-" + UUID.randomUUID() + "@example.test";
    }

    @Test void authorizationRedirectHasStateNonceAndPkceButNoSecret() throws Exception {
        var result = mvc.perform(get("/oauth2/authorization/google")).andExpect(status().is3xxRedirection()).andReturn();
        assertThat(result.getResponse().getRedirectedUrl()).startsWith("https://accounts.google.com/")
                .contains("state=", "nonce=", "code_challenge=", "code_challenge_method=S256").doesNotContain("test-only-not-a-real-secret");
        assertThat(result.getRequest().getSession().getAttribute("SPRING_SECURITY_CONTEXT")).isNull();
        mvc.perform(get("/login/oauth2/code/google").param("code", "invalid").param("state", "wrong"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login?googleError"));
    }

    @Test void verifiedNewIdentityRequiresProfileThenGetsShopCookieAndClientRole() throws Exception {
        var session = pending(true);
        mvc.perform(get("/onboarding/google").session(session)).andExpect(status().isOk()).andExpect(view().name("oauth/google-profile"));
        mvc.perform(post("/onboarding/google").session(session).param("name", "New client").param("email", email).param("card", "card"))
                .andExpect(status().isForbidden());
        var result = mvc.perform(post("/onboarding/google").session(session).with(csrf())
                .param("name", "New client").param("email", email).param("card", "card").param("role", "EMPLOYEE").param("id", "9"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/")).andReturn();
        var account = users.findByEmailIgnoreCase(email).orElseThrow();
        assertThat(account).isInstanceOf(Client.class);
        assertThat(identities.findAccount(subject)).contains(account.getId());
        var access = result.getResponse().getCookie(AuthTokenCookies.NAME);
        assertThat(access).isNotNull(); assertThat(access.isHttpOnly()).isTrue();
        assertThat(decoder.decode(access.getValue()).getClaimAsStringList("roles")).containsExactly("CLIENT");
        assertThat(session.isInvalid()).isTrue();
        mvc.perform(get("/orders").cookie(access)).andExpect(status().isOk());
    }

    @Test void changedEmailRequiresMailpitConfirmationInTheSameBrowser() throws Exception {
        var session = pending(true);
        String chosen = "chosen-" + UUID.randomUUID() + "@example.test";
        mvc.perform(post("/onboarding/google").session(session).with(csrf()).param("name", "New client")
                .param("email", chosen).param("card", "card")).andExpect(status().isOk()).andExpect(model().attribute("verificationSent", true));
        assertThat(users.findByEmailIgnoreCase(chosen)).isEmpty();
        var uri = ArgumentCaptor.forClass(URI.class);
        verify(mail, timeout(3000)).sendEmailVerification(eq(chosen), uri.capture(), any(Locale.class));
        String token = uri.getValue().getRawQuery().substring("token=".length());
        mvc.perform(get("/onboarding/google/verify").param("token", token)).andExpect(redirectedUrl("/login?googleExpired"));
        mvc.perform(get("/onboarding/google/verify").session(session).param("token", token))
                .andExpect(redirectedUrl("/onboarding/google?verified"));
        mvc.perform(post("/onboarding/google").session(session).with(csrf()).param("name", "New client")
                .param("email", chosen).param("card", "card")).andExpect(redirectedUrl("/"));
        assertThat(users.findByEmailIgnoreCase(chosen)).isPresent();
    }

    @Test void emailCollisionDoesNotAutomaticallyLinkAndKnownSubjectSurvivesEmailChanges() throws Exception {
        var client = createClient(email);
        var request = new MockHttpServletRequest(); var response = new MockHttpServletResponse();
        handler().onAuthenticationSuccess(request, response, oidc());
        assertThat(response.getRedirectedUrl()).isEqualTo("/login?googleLinkRequired");
        assertThat(identities.findAccount(subject)).isEmpty();
        identities.link(client.getId(), new VerifiedGoogleIdentity(subject, email, "Google Name", true));
        client.setEmail("changed-" + UUID.randomUUID() + "@example.test"); clients.saveAndFlush(client);
        response = new MockHttpServletResponse();
        handler().onAuthenticationSuccess(new MockHttpServletRequest(), response, oidc());
        assertThat(response.getRedirectedUrl()).isEqualTo("/");
        assertThat(decoder.decode(response.getCookie(AuthTokenCookies.NAME).getValue()).getSubject()).isEqualTo(client.getId().toString());
    }

    @Test void googleCreatedAccountCanSetKnownPasswordThroughItsOwnEmail() throws Exception {
        var profile = new com.epam.rd.autocode.assessment.appliances.dto.oauth.GoogleProfileRequest();
        profile.setName("Google password setup"); profile.setEmail(email); profile.setCard("card");
        Long id = identities.createProfile(new VerifiedGoogleIdentity(subject, email, "Name", true), profile);
        verify(mail, timeout(3000)).sendAccountCreated(eq(email), any(Locale.class));
        Cookie access = access(id);
        mvc.perform(get("/password/change").cookie(access)).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/password/setup")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("name=\"currentPassword\""))));
        mvc.perform(post("/password/setup").cookie(access)).andExpect(status().isForbidden());
        mvc.perform(post("/password/setup").cookie(access).with(csrf()).param("email", "someone-else@example.test"))
                .andExpect(redirectedUrl("/password/change?setupSent"));
        var uri = ArgumentCaptor.forClass(URI.class);
        verify(mail, timeout(3000)).sendPasswordReset(eq(email), uri.capture(), any(Locale.class));
        verify(mail, never()).sendPasswordReset(eq("someone-else@example.test"), any(), any());
        String token = uri.getValue().getRawQuery().substring("token=".length());
        var rollbackForm = new com.epam.rd.autocode.assessment.appliances.dto.password.PasswordResetRequest();
        rollbackForm.setToken(token); rollbackForm.setNewPassword("KnownStorePass2026!"); rollbackForm.setConfirmPassword("KnownStorePass2026!");
        new org.springframework.transaction.support.TransactionTemplate(transactions).executeWithoutResult(status -> {
            resetPasswords.reset(rollbackForm); status.setRollbackOnly();
        });
        assertThat(passwordState.status(id)).isEqualTo(com.epam.rd.autocode.assessment.appliances.security.password.PasswordCredentialStatus.NOT_SET);
        verify(mail, never()).sendPasswordChanged(eq(email), any(Locale.class));
        mvc.perform(post("/password/reset").with(csrf()).param("token", token)
                .param("newPassword", "KnownStorePass2026!").param("confirmPassword", "KnownStorePass2026!"))
                .andExpect(redirectedUrl("/login?passwordChanged"));
        assertThat(encoder.matches("KnownStorePass2026!", users.findById(id).orElseThrow().getPassword())).isTrue();
        verify(mail, timeout(3000)).sendPasswordChanged(eq(email), any(Locale.class));
        Cookie afterSetup = access(id);
        mvc.perform(get("/password/change").cookie(afterSetup)).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"currentPassword\"")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("action=\"/password/setup\""))));
    }

    @Test void legacyGoogleAccountStartsWithSetupButCanChooseItsExistingPasswordForm() throws Exception {
        var client = createClient(email);
        links.saveAndFlush(new com.epam.rd.autocode.assessment.appliances.model.oauth.OAuthIdentity(subject, client.getId()));
        Cookie access = access(client.getId());
        mvc.perform(get("/password/change").cookie(access))
                .andExpect(status().isOk()).andExpect(model().attribute("showPasswordChangeForm", false))
                .andExpect(model().attribute("passwordStateUnknown", true));
        mvc.perform(get("/password/change").param("mode", "change").cookie(access))
                .andExpect(status().isOk()).andExpect(model().attribute("showPasswordChangeForm", true));
    }

    @Test void linkingRequiresCsrfAndTheSameBrowserAccount() throws Exception {
        var first = createClient(email);
        Cookie access = access(first.getId());
        mvc.perform(post("/account/google/link").cookie(access)).andExpect(status().isForbidden());
        var result = mvc.perform(post("/account/google/link").cookie(access).with(csrf()))
                .andExpect(redirectedUrl("/oauth2/authorization/google")).andReturn();
        var request = new MockHttpServletRequest(); request.setSession((MockHttpSession) result.getRequest().getSession()); request.setCookies(access);
        var response = new MockHttpServletResponse(); handler().onAuthenticationSuccess(request, response, oidc());
        assertThat(response.getRedirectedUrl()).isEqualTo("/account/google?linked");
        assertThat(identities.findAccount(subject)).contains(first.getId());
        var second = createClient("other-" + UUID.randomUUID() + "@example.test");
        assertThatThrownBy(() -> identities.link(second.getId(), new VerifiedGoogleIdentity(subject, email, "Name", true)))
                .isInstanceOf(GoogleLoginException.class);
    }

    private MockHttpSession pending(boolean verified) {
        var request = new MockHttpServletRequest();
        flows.pending(request, new VerifiedGoogleIdentity(subject, email, "Google Name", verified));
        return (MockHttpSession) request.getSession();
    }
    private Client createClient(String address) {
        return clients.saveAndFlush(new Client(null, "Existing", address, encoder.encode("ExistingLocal2026!"), "card"));
    }
    private Cookie access(Long id) { return new Cookie(AuthTokenCookies.NAME, tokens.issue(accounts.forAccount(id)).access().accessToken()); }
    private GoogleLoginSuccessHandler handler() {
        return new GoogleLoginSuccessHandler(verifier, identities, accounts, flows, tokens, cookies, browserCookies,
                new GoogleLinkVerifier(browserCookies, decoder, hashes));
    }
    private OAuth2AuthenticationToken oidc() {
        var claims = new HashMap<String, Object>();
        claims.put("iss", "https://accounts.google.com"); claims.put("sub", subject); claims.put("email", email); claims.put("email_verified", true);
        var user = new DefaultOidcUser(List.of(new SimpleGrantedAuthority("OIDC_USER")),
                new OidcIdToken("test-only-id-token", Instant.now(), Instant.now().plusSeconds(600), claims));
        return new OAuth2AuthenticationToken(user, user.getAuthorities(), "google");
    }
}
