package com.epam.rd.autocode.assessment.appliances.security.jwt.web;

import com.epam.rd.autocode.assessment.appliances.api.error.ApiProblems;
import com.epam.rd.autocode.assessment.appliances.api.error.ApiSecurityFailures;
import com.epam.rd.autocode.assessment.appliances.repository.RefreshTokenRepository;
import com.epam.rd.autocode.assessment.appliances.repository.UserRepository;
import com.epam.rd.autocode.assessment.appliances.security.identity.CurrentActor;
import com.epam.rd.autocode.assessment.appliances.security.jwt.access.AccessTokenAuthentication;
import com.epam.rd.autocode.assessment.appliances.security.jwt.access.AccessTokenIssuer;
import com.epam.rd.autocode.assessment.appliances.security.jwt.access.AccessTokenPrincipal;
import com.epam.rd.autocode.assessment.appliances.security.jwt.cookie.AuthTokenCookies;
import com.epam.rd.autocode.assessment.appliances.security.jwt.cookie.JwtCookieLifecycle;
import com.epam.rd.autocode.assessment.appliances.security.jwt.refresh.InvalidRefreshTokenException;
import com.epam.rd.autocode.assessment.appliances.security.jwt.refresh.RefreshTokenCodec;
import com.epam.rd.autocode.assessment.appliances.security.jwt.refresh.RefreshTokenProperties;
import com.epam.rd.autocode.assessment.appliances.security.jwt.refresh.RefreshTokenService;
import com.epam.rd.autocode.assessment.appliances.security.login.PasswordLoginFailureHandler;
import com.epam.rd.autocode.assessment.appliances.security.web.MvcSecurityFailures;
import com.epam.rd.autocode.assessment.appliances.web.error.MvcRouteClassifier;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthenticationAvailabilityTest {
    @AfterEach void clearContext() { SecurityContextHolder.clearContext(); }

    @Test void passwordBackendFailureIs503WithoutCredentialErrorRedirectOrSession() throws Exception {
        var request = new MockHttpServletRequest("POST", "/login"); var response = new MockHttpServletResponse();
        new PasswordLoginFailureHandler().onAuthenticationFailure(request, response, new AuthenticationServiceException("private backend detail"));
        assertThat(response.getStatus()).isEqualTo(503); assertThat(response.getRedirectedUrl()).isNull();
        assertThat(response.getErrorMessage()).isNull(); assertThat(request.getSession(false)).isNull();
    }

    @Test void jwtAccountLookupOutageKeepsCookiesAndDoesNotBecomeUnauthorized() throws Exception {
        var actor = mock(CurrentActor.class); var cookies = mock(AuthTokenCookies.class);
        var mvc = mock(MvcSecurityFailures.class); var routes = mock(MvcRouteClassifier.class); var chain = mock(FilterChain.class);
        when(actor.requireUser()).thenThrow(new CannotCreateTransactionException("private connection detail"));
        var auth = new AccessTokenAuthentication(new AccessTokenPrincipal(1L), List.of(), Instant.now().plusSeconds(60));
        SecurityContextHolder.getContext().setAuthentication(auth);
        var request = new MockHttpServletRequest("GET", "/orders"); var response = new MockHttpServletResponse();
        new JwtAccountFilter(actor, cookies, new JwtBrowserFailures(cookies, mvc, routes), true).doFilter(request, response, chain);
        assertThat(response.getStatus()).isEqualTo(503); assertThat(response.getRedirectedUrl()).isNull();
        verifyNoInteractions(cookies, mvc, routes, chain);
    }

    @Test void apiAuthenticationOutageIsSafe503WithoutBearerChallenge() throws Exception {
        var problems = mock(ApiProblems.class); var request = new MockHttpServletRequest("GET", "/api/orders");
        when(problems.create(eq(HttpStatus.SERVICE_UNAVAILABLE), eq("http.503"), eq("error.503.message"), same(request)))
                .thenReturn(ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, "Temporarily unavailable"));
        var response = new MockHttpServletResponse();
        new ApiSecurityFailures(problems, new ObjectMapper()).commence(request, response, new AuthenticationServiceException("private backend detail"));
        assertThat(response.getStatus()).isEqualTo(503); assertThat(response.getHeader("WWW-Authenticate")).isNull();
        assertThat(response.getContentAsString()).doesNotContain("private backend detail");
    }

    @Test void refreshTransactionFailureIsNotReportedAsAnInvalidCredentialAndHasSanitizedCause() {
        var manager = mock(PlatformTransactionManager.class);
        when(manager.getTransaction(any(TransactionDefinition.class))).thenThrow(new CannotCreateTransactionException("private connection detail"));
        var codec = new RefreshTokenCodec();
        var refresh = new RefreshTokenService(mock(RefreshTokenRepository.class), mock(UserRepository.class), codec,
                new RefreshTokenProperties(Duration.ofDays(7), Duration.ofDays(8), 3600000), mock(AccessTokenIssuer.class), Clock.systemUTC(), manager);
        assertThatThrownBy(() -> refresh.rotate(codec.generate())).isInstanceOf(AuthenticationServiceException.class)
                .isNotInstanceOf(InvalidRefreshTokenException.class).satisfies(failure -> assertThat(failure.getCause()).hasMessageNotContaining("private connection detail"));
    }

    @Test void logoutStorageFailureClearsBrowserButDoesNotPretendServerRevocationSucceeded() throws Exception {
        var refresh = mock(RefreshTokenService.class); var cookies = mock(AuthTokenCookies.class); var lifecycle = mock(JwtCookieLifecycle.class);
        var request = new MockHttpServletRequest("POST", "/logout"); var response = new MockHttpServletResponse();
        when(cookies.readRefresh(request)).thenReturn("opaque-refresh");
        doThrow(new AuthenticationServiceException("unavailable")).when(refresh).revoke("opaque-refresh");
        new RefreshLogoutHandler(refresh, cookies, lifecycle).logout(request, response, null);
        new JwtLogoutSuccessHandler().onLogoutSuccess(request, response, null);
        verify(lifecycle).clear(request, response);
        assertThat(response.getStatus()).isEqualTo(503); assertThat(response.getRedirectedUrl()).isNull();
    }
}
