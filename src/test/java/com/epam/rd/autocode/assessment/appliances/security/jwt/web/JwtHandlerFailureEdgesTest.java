package com.epam.rd.autocode.assessment.appliances.security.jwt.web;

import static org.mockito.Mockito.*;

import com.epam.rd.autocode.assessment.appliances.security.jwt.cookie.*;
import com.epam.rd.autocode.assessment.appliances.security.jwt.refresh.RefreshTokenService;
import com.epam.rd.autocode.assessment.appliances.security.web.MvcSecurityFailures;
import com.epam.rd.autocode.assessment.appliances.web.error.MvcRouteClassifier;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.*;
import org.springframework.security.authentication.BadCredentialsException;

class JwtHandlerFailureEdgesTest {
    @Test
    void malformedRefreshCookiesAreClearedAndNeverBlockLogoutOrPasswordLogin() throws Exception {
        var cookies = mock(AuthTokenCookies.class);
        var lifecycle = mock(JwtCookieLifecycle.class);
        var tokens = mock(RefreshTokenService.class);
        var request = new MockHttpServletRequest();
        var response = new MockHttpServletResponse();
        when(cookies.readRefresh(request)).thenThrow(new IllegalArgumentException("private"));
        new RefreshLogoutHandler(tokens, cookies, lifecycle).logout(request, response, null);
        verify(tokens).revoke(null);
        verify(lifecycle).clear(request, response);
        new JwtLoginSuccessHandler(tokens, lifecycle, cookies)
                .onAuthenticationSuccess(request, response, null);
        verify(tokens).issue(null, null);
        var failures = mock(MvcSecurityFailures.class);
        var routes = mock(MvcRouteClassifier.class);
        new JwtBrowserFailures(cookies, failures, routes)
                .commence(request, response, new BadCredentialsException("bad"));
        verify(cookies).clear(request, response);
        verify(failures).commence(eq(request), eq(response), any());
    }
}
