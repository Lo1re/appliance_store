package com.epam.rd.autocode.assessment.appliances.api.controller;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.epam.rd.autocode.assessment.appliances.dto.auth.*;
import com.epam.rd.autocode.assessment.appliances.security.jwt.TokenPair;
import com.epam.rd.autocode.assessment.appliances.security.jwt.api.*;
import com.epam.rd.autocode.assessment.appliances.security.jwt.cookie.*;
import com.epam.rd.autocode.assessment.appliances.security.jwt.refresh.*;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.*;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;

class ApiAuthenticationControllerBranchTest {
    final ApiPasswordLogin passwords = mock(ApiPasswordLogin.class);
    final RefreshTokenService refresh = mock(RefreshTokenService.class);
    final AuthTokenCookies browser = mock(AuthTokenCookies.class);
    final JwtCookieLifecycle cookies = mock(JwtCookieLifecycle.class);
    final MockHttpServletRequest request = new MockHttpServletRequest();
    final MockHttpServletResponse response = new MockHttpServletResponse();
    final TokenPair pair =
            new TokenPair(
                    new AccessTokenResponse(
                            "access", "Bearer", 900, Instant.now().plusSeconds(900)),
                    "refresh",
                    Instant.now().plusSeconds(3600));

    ApiAuthenticationController controller(boolean cookie) {
        return new ApiAuthenticationController(
                passwords,
                refresh,
                browser,
                cookies,
                new ApiTransportProperties(
                        cookie
                                ? ApiTransportProperties.Transport.COOKIE
                                : ApiTransportProperties.Transport.HEADER));
    }

    @Test
    void csrfEndpointExistsOnlyInCookieModeWithAvailableToken() {
        var token = new DefaultCsrfToken("X-CSRF", "_csrf", "value");
        assertThatThrownBy(() -> controller(false).csrf(token))
                .isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> controller(true).csrf(null))
                .isInstanceOf(ResponseStatusException.class);
        assertThat(controller(true).csrf(token).getBody().token()).isEqualTo("value");
    }

    @Test
    void headerRefreshAcceptsBodyAndMissingBodyWithoutTouchingCookies() {
        when(refresh.rotate(any())).thenReturn(pair);
        assertThat(
                        controller(false)
                                .refresh(new RefreshTokenRequest("raw"), request, response)
                                .getBody())
                .isInstanceOf(TokenPairResponse.class);
        controller(false).refresh(null, request, response);
        verify(refresh).rotate((String) null);
        verifyNoInteractions(cookies, browser);
    }

    @Test
    void malformedCookieCannotBeRefreshedButPasswordLoginCanReplaceIt() {
        when(browser.readRefresh(request)).thenThrow(new IllegalArgumentException("private"));
        assertThatThrownBy(() -> controller(true).refresh(null, request, response))
                .isInstanceOf(InvalidRefreshTokenException.class);
        verify(cookies).clear(request, response);
        when(passwords.login(any(), any(), any())).thenReturn(pair);
        assertThat(controller(true).login(new ApiLoginRequest(), request, response).getBody())
                .isInstanceOf(AccessTokenMetadata.class);
        verify(cookies).issue(request, response, pair);
    }

    @Test
    void invalidHeaderRefreshDoesNotClearBrowserCookies() {
        when(refresh.rotate("raw")).thenThrow(new InvalidRefreshTokenException());
        assertThatThrownBy(
                        () ->
                                controller(false)
                                        .refresh(new RefreshTokenRequest("raw"), request, response))
                .isInstanceOf(InvalidRefreshTokenException.class);
        verifyNoInteractions(cookies);
    }

    @Test
    void cookieTransportRejectsBodyCredentialsAndLogoutStillClearsCookies() {
        assertThatThrownBy(
                        () ->
                                controller(true)
                                        .refresh(new RefreshTokenRequest("raw"), request, response))
                .isInstanceOf(InvalidRefreshTokenException.class);
        assertThat(
                        controller(true)
                                .logout(new RefreshTokenRequest("raw"), request, response)
                                .getStatusCode()
                                .value())
                .isEqualTo(204);
        verify(refresh).revoke(null);
    }

    @Test
    void headerLogoutDoesNotAlterCookies() {
        assertThat(
                        controller(false)
                                .logout(new RefreshTokenRequest("raw"), request, response)
                                .getStatusCode()
                                .value())
                .isEqualTo(204);
        verify(refresh).revoke("raw");
        verifyNoInteractions(cookies);
    }
}
