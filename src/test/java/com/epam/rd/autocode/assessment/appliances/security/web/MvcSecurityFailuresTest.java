package com.epam.rd.autocode.assessment.appliances.security.web;

import com.epam.rd.autocode.assessment.appliances.web.error.MvcRouteClassifier;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MvcSecurityFailuresTest {
    private final RequestMappingHandlerMapping routes = mock(RequestMappingHandlerMapping.class);
    private final MvcSecurityFailures failures = new MvcSecurityFailures(new MvcRouteClassifier(routes));

    @Test
    void unknownReadIs404AndKnownDeniedRouteIs403() throws Exception {
        var request = new MockHttpServletRequest("GET", "/unknown");
        var response = new MockHttpServletResponse();
        failures.handle(request, response, new AccessDeniedException("denied"));
        assertThat(response.getStatus()).isEqualTo(404);
        when(routes.getHandler(any())).thenReturn(new HandlerExecutionChain(new Object()));
        response = new MockHttpServletResponse();
        failures.handle(request, response, new AccessDeniedException("denied"));
        assertThat(response.getStatus()).isEqualTo(403);
    }

    @Test
    void csrfFailureOnAnUnsafeMethodRemains403WithoutRouteLookup() throws Exception {
        var response = new MockHttpServletResponse();
        failures.handle(new MockHttpServletRequest("POST", "/unknown"), response,
                new AccessDeniedException("denied"));
        assertThat(response.getStatus()).isEqualTo(403);
        verifyNoInteractions(routes);
    }

    @Test
    void routeLookupFailureNeverGrantsAccess() throws Exception {
        when(routes.getHandler(any())).thenThrow(new HttpRequestMethodNotSupportedException("GET"));
        var response = new MockHttpServletResponse();
        failures.handle(new MockHttpServletRequest("GET", "/known"), response,
                new AccessDeniedException("denied"));
        assertThat(response.getStatus()).isEqualTo(403);
    }

    @Test
    void anonymousUnknownReadGets404InsteadOfALoginRedirect() throws Exception {
        var response = new MockHttpServletResponse();
        failures.commence(new MockHttpServletRequest("HEAD", "/unknown"), response,
                new InsufficientAuthenticationException("login required"));
        assertThat(response.getStatus()).isEqualTo(404);
        assertThat(response.getRedirectedUrl()).isNull();
    }
}
