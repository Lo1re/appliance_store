package com.epam.rd.autocode.assessment.appliances.security.oauth.google.web;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.epam.rd.autocode.assessment.appliances.security.jwt.cookie.*;
import com.epam.rd.autocode.assessment.appliances.security.jwt.refresh.RefreshTokenService;
import com.epam.rd.autocode.assessment.appliances.security.oauth.google.flow.*;
import com.epam.rd.autocode.assessment.appliances.security.oauth.google.identity.*;

import org.junit.jupiter.api.Test;
import org.springframework.dao.*;
import org.springframework.mock.web.*;
import org.springframework.security.authentication.*;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.*;

class GoogleCompletionEdgesTest {
    final GoogleIdentityVerifier verifier = mock(GoogleIdentityVerifier.class);
    final GoogleIdentityService identities = mock(GoogleIdentityService.class);
    final GoogleAccountAuthentication accounts = mock(GoogleAccountAuthentication.class);
    final GoogleFlowSession flows = mock(GoogleFlowSession.class);
    final RefreshTokenService tokens = mock(RefreshTokenService.class);
    final JwtCookieLifecycle cookies = mock(JwtCookieLifecycle.class);
    final AuthTokenCookies browser = mock(AuthTokenCookies.class);
    final GoogleLinkVerifier links = mock(GoogleLinkVerifier.class);
    final GoogleLoginSuccessHandler handler =
            new GoogleLoginSuccessHandler(
                    verifier, identities, accounts, flows, tokens, cookies, browser, links);

    OAuth2AuthenticationToken login() {
        var oidc = mock(OidcUser.class);
        when(oidc.getAttributes()).thenReturn(Map.of("sub", "subject"));
        var auth = new OAuth2AuthenticationToken(oidc, List.of(), "google");
        when(verifier.verify(oidc))
                .thenReturn(
                        new VerifiedGoogleIdentity("subject", "user@example.test", "User", true));
        return auth;
    }

    @Test
    void malformedPrincipalOrProviderFinishesTheTemporaryFlow() throws Exception {
        var request = new MockHttpServletRequest();
        var response = new MockHttpServletResponse();
        handler.onAuthenticationSuccess(
                request,
                response,
                UsernamePasswordAuthenticationToken.unauthenticated("user", "secret"));
        assertThat(response.getRedirectedUrl()).isEqualTo("/login?googleError");
        response = new MockHttpServletResponse();
        handler.onAuthenticationSuccess(
                request,
                response,
                new OAuth2AuthenticationToken(mock(OAuth2User.class), List.of(), "other"));
        assertThat(response.getRedirectedUrl()).isEqualTo("/login?googleError");
        response = new MockHttpServletResponse();
        handler.onAuthenticationSuccess(
                request,
                response,
                new OAuth2AuthenticationToken(mock(OAuth2User.class), List.of(), "google"));
        assertThat(response.getRedirectedUrl()).isEqualTo("/login?googleError");
    }

    @Test
    void verifiedKnownEmailRequiresAnExplicitLinkButUnverifiedEmailCanOnboard() throws Exception {
        var auth = login();
        when(identities.emailTaken("user@example.test")).thenReturn(true);
        var response = new MockHttpServletResponse();
        handler.onAuthenticationSuccess(new MockHttpServletRequest(), response, auth);
        assertThat(response.getRedirectedUrl()).isEqualTo("/login?googleLinkRequired");
        when(verifier.verify(any()))
                .thenReturn(
                        new VerifiedGoogleIdentity("subject", "user@example.test", "User", false));
        response = new MockHttpServletResponse();
        handler.onAuthenticationSuccess(new MockHttpServletRequest(), response, auth);
        assertThat(response.getRedirectedUrl()).isEqualTo("/onboarding/google");
        when(verifier.verify(any()))
                .thenReturn(
                        new VerifiedGoogleIdentity("subject", "other@example.test", "User", true));
        response = new MockHttpServletResponse();
        handler.onAuthenticationSuccess(new MockHttpServletRequest(), response, auth);
        assertThat(response.getRedirectedUrl()).isEqualTo("/onboarding/google");
    }

    @Test
    void persistenceConflictsOutagesAndUnexpectedFailuresAreSanitized() throws Exception {
        var auth = login();
        for (RuntimeException failure :
                new RuntimeException[] {
                    new DataIntegrityViolationException("private"),
                    new DataAccessResourceFailureException("private"),
                    new IllegalStateException("private")
                }) {
            doThrow(failure).when(identities).findAccount("subject");
            var request = new MockHttpServletRequest();
            var response = new MockHttpServletResponse();
            handler.onAuthenticationSuccess(request, response, auth);
            if (failure instanceof DataAccessResourceFailureException) {
                assertThat(response.getStatus()).isEqualTo(503);
            } else {
                assertThat(response.getRedirectedUrl()).isEqualTo("/login?googleError");
            }
            verify(flows).finish(request, response);
        }
    }
}
