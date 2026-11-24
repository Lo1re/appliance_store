package com.epam.rd.autocode.assessment.appliances.api.error;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.epam.rd.autocode.assessment.appliances.logging.RequestLogContext;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.mock.web.*;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.web.ErrorResponseException;

import java.io.IOException;
import java.util.Locale;

class ApiFailureBoundaryEdgesTest {
    ApiProblems problems() {
        var source = new StaticMessageSource();
        for (String key :
                new String[] {
                    "error.4xx.title", "error.4xx.message", "error.5xx.title", "error.5xx.message"
                }) {
            source.addMessage(key, Locale.ENGLISH, "Safe fallback");
        }
        return new ApiProblems(source);
    }

    @Test
    void unknownStatusesAndRequestIdentifiersHaveSafeFallbackBodies() {
        var problems = problems();
        var request = new MockHttpServletRequest();
        request.setAttribute(RequestLogContext.ATTRIBUTE, "request-id");
        var server = problems.create(HttpStatusCode.valueOf(599), "code", "missing", request);
        assertThat(server.getTitle()).isEqualTo("Safe fallback");
        assertThat(server.getProperties().get("requestId")).isEqualTo("request-id");
        assertThat(
                        problems.create(
                                        HttpStatusCode.valueOf(418),
                                        "code",
                                        "missing",
                                        new MockHttpServletRequest())
                                .getStatus())
                .isEqualTo(418);
    }

    @Test
    void malformedBodiesExpiredCredentialsAndUnknownFrameworkErrorsKeepTheRightStatus() {
        var mapper = new ApiExceptionMapper(problems());
        var request = new MockHttpServletRequest();
        assertThat(
                        mapper.handle(
                                        new HttpMessageNotReadableException(
                                                "private", new MockHttpInputMessage(new byte[0])),
                                        request)
                                .getStatusCode()
                                .value())
                .isEqualTo(400);
        assertThat(
                        mapper.handle(new CredentialsExpiredException("private"), request)
                                .getStatusCode()
                                .value())
                .isEqualTo(401);
        var error = new ErrorResponseException(HttpStatus.I_AM_A_TEAPOT);
        error.getHeaders().set("Allow", "GET");
        assertThat(mapper.handle(error, request).getHeaders().getFirst("Allow")).isEqualTo("GET");
        assertThat(
                        mapper.handle(new IllegalStateException("private"), request)
                                .getStatusCode()
                                .value())
                .isEqualTo(500);
    }

    @Test
    void rootApiErrorsAreResolvedAndOutputFailureDoesNotExposeTheException() throws Exception {
        var mapper = mock(ApiExceptionMapper.class);
        when(mapper.handle(any(), any()))
                .thenReturn(ResponseEntity.status(500).body(ProblemDetail.forStatus(500)));
        var resolver = new ApiExceptionResolver(mapper, new ObjectMapper());
        assertThat(
                        resolver.resolveException(
                                new MockHttpServletRequest("GET", "/apix"),
                                new MockHttpServletResponse(),
                                null,
                                new Exception()))
                .isNull();
        var request = new MockHttpServletRequest("GET", "/api");
        var response = mock(HttpServletResponse.class);
        when(response.getOutputStream()).thenThrow(new IOException("private"));
        assertThat(resolver.resolveException(request, response, null, new Exception())).isNotNull();
    }

    @Test
    void unavailableAuthenticationAndValidationErrorsMapWithoutDetails() {
        var mapper = new ApiExceptionMapper(problems());
        var r = new MockHttpServletRequest();
        assertThat(
                        mapper.handle(
                                        new org.springframework.security.authentication
                                                .AuthenticationServiceException("private"),
                                        r)
                                .getStatusCode()
                                .value())
                .isEqualTo(503);
        assertThat(
                        mapper.handle(
                                        new jakarta.validation.ConstraintViolationException(
                                                java.util.Set.of()),
                                        r)
                                .getStatusCode()
                                .value())
                .isEqualTo(400);
        assertThat(
                        mapper.handle(
                                        new org.springframework.beans.TypeMismatchException(
                                                "private", Integer.class),
                                        r)
                                .getStatusCode()
                                .value())
                .isEqualTo(400);
    }
}
