package com.epam.rd.autocode.assessment.appliances.api.error;

import com.epam.rd.autocode.assessment.appliances.exception.BusinessException;
import com.epam.rd.autocode.assessment.appliances.logging.SafeFailure;
import com.epam.rd.autocode.assessment.appliances.security.jwt.api.PasswordLoginLockedException;
import com.epam.rd.autocode.assessment.appliances.security.jwt.refresh.InvalidRefreshTokenException;
import com.epam.rd.autocode.assessment.appliances.web.error.BusinessErrorStatus;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.TypeMismatchException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindException;
import org.springframework.web.ErrorResponse;

import java.util.LinkedHashMap;
import java.util.List;

@Slf4j
@Component
@ConditionalOnProperty(name = "store.api.enabled", havingValue = "true")
@RequiredArgsConstructor
public class ApiExceptionMapper {
    private static final List<StatusRule> STATUS_RULES =
            List.of(
                    new StatusRule(LockedException.class, HttpStatus.TOO_MANY_REQUESTS),
                    new StatusRule(
                            AuthenticationServiceException.class, HttpStatus.SERVICE_UNAVAILABLE),
                    new StatusRule(AuthenticationException.class, HttpStatus.UNAUTHORIZED),
                    new StatusRule(AccessDeniedException.class, HttpStatus.FORBIDDEN),
                    new StatusRule(BindException.class, HttpStatus.BAD_REQUEST),
                    new StatusRule(ConstraintViolationException.class, HttpStatus.BAD_REQUEST),
                    new StatusRule(TypeMismatchException.class, HttpStatus.BAD_REQUEST),
                    new StatusRule(HttpMessageNotReadableException.class, HttpStatus.BAD_REQUEST));

    private final ApiProblems problems;

    public ResponseEntity<ProblemDetail> handle(Exception failure, HttpServletRequest request) {
        ApiFailure mapped = classify(failure);
        HttpHeaders headers = headers(mapped, failure);
        var problem = problems.create(mapped.status(), mapped.code(), mapped.messageKey(), request);
        addFieldErrors(problem, failure);
        logFailure(mapped.status(), mapped.code(), failure);
        return new ResponseEntity<>(problem, headers, mapped.status());
    }

    private ApiFailure classify(Exception failure) {
        if (failure instanceof BusinessException business) {
            return new ApiFailure(BusinessErrorStatus.of(business), business.error().messageKey());
        }
        if (failure instanceof PasswordLoginLockedException) {
            return new ApiFailure(HttpStatus.TOO_MANY_REQUESTS, "login.locked");
        }
        if (failure instanceof InvalidRefreshTokenException) {
            return new ApiFailure(HttpStatus.UNAUTHORIZED, "refresh.invalid");
        }
        if (failure instanceof BadCredentialsException) {
            return new ApiFailure(HttpStatus.UNAUTHORIZED, "login.error");
        }
        HttpStatusCode status = status(failure);
        return new ApiFailure(
                status, "http." + status.value(), "error." + status.value() + ".message");
    }

    private HttpStatusCode status(Exception failure) {
        for (StatusRule rule : STATUS_RULES) {
            if (rule.type().isInstance(failure)) {
                return rule.status();
            }
        }
        return failure instanceof ErrorResponse error
                ? error.getStatusCode()
                : HttpStatus.INTERNAL_SERVER_ERROR;
    }

    private HttpHeaders headers(ApiFailure mapped, Exception failure) {
        var headers = new HttpHeaders();
        if (failure instanceof ErrorResponse error) {
            headers.addAll(error.getHeaders());
        }
        if (mapped.status().value() == 401) {
            headers.set(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        }
        if (failure instanceof PasswordLoginLockedException locked) {
            headers.set(HttpHeaders.RETRY_AFTER, Long.toString(locked.getRetryAfterSeconds()));
        }
        headers.setCacheControl("no-store");
        headers.setContentType(MediaType.APPLICATION_PROBLEM_JSON);
        return headers;
    }

    private void addFieldErrors(ProblemDetail problem, Exception failure) {
        if (failure instanceof BindException binding) {
            var fields = new LinkedHashMap<String, String>();
            binding.getFieldErrors()
                    .forEach(error -> fields.putIfAbsent(error.getField(), "invalid"));
            problem.setProperty("fields", fields);
        }
    }

    private void logFailure(HttpStatusCode status, String code, Exception failure) {
        if (status.is5xxServerError()) {
            log.error("Unexpected API failure", SafeFailure.snapshot(failure));
        } else {
            log.debug("API request rejected (status={}, code={})", status.value(), code);
        }
    }

    private record ApiFailure(HttpStatusCode status, String code, String messageKey) {
        private ApiFailure(HttpStatusCode status, String code) {
            this(status, code, code);
        }
    }

    private record StatusRule(Class<? extends Exception> type, HttpStatus status) {
    }
}
