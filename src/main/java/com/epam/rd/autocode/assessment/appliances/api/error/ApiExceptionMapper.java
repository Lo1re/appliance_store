package com.epam.rd.autocode.assessment.appliances.api.error;

import com.epam.rd.autocode.assessment.appliances.exception.BusinessException;
import com.epam.rd.autocode.assessment.appliances.logging.SafeFailure;
import com.epam.rd.autocode.assessment.appliances.security.jwt.PasswordLoginLockedException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.util.LinkedHashMap;
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

@Slf4j
@Component
@ConditionalOnProperty(name = "store.api.enabled", havingValue = "true")
@RequiredArgsConstructor
public class ApiExceptionMapper {
    private final ApiProblems problems;

    public ResponseEntity<ProblemDetail> handle(Exception failure, HttpServletRequest request) {
        HttpStatusCode status;
        String code; String key;
        var headers = new HttpHeaders();
        if (failure instanceof BusinessException business) {
            status = switch (business.error().kind()) {
                case NOT_FOUND -> HttpStatus.NOT_FOUND;
                case VALIDATION -> HttpStatus.BAD_REQUEST;
                case CONFLICT, BUSINESS_RULE -> HttpStatus.CONFLICT;
            };
            code = business.error().messageKey(); key = code;
        } else {
            if (failure instanceof LockedException) status = HttpStatus.TOO_MANY_REQUESTS;
            else if (failure instanceof AuthenticationServiceException) status = HttpStatus.SERVICE_UNAVAILABLE;
            else if (failure instanceof AuthenticationException) status = HttpStatus.UNAUTHORIZED;
            else if (failure instanceof AccessDeniedException) status = HttpStatus.FORBIDDEN;
            else if (failure instanceof BindException || failure instanceof ConstraintViolationException
                    || failure instanceof TypeMismatchException || failure instanceof HttpMessageNotReadableException) status = HttpStatus.BAD_REQUEST;
            else if (failure instanceof ErrorResponse error) {
                status = error.getStatusCode(); headers.addAll(error.getHeaders());
            } else status = HttpStatus.INTERNAL_SERVER_ERROR;
            code = "http." + status.value(); key = "error." + status.value() + ".message";
        }
        if (failure instanceof BadCredentialsException) { code = "login.error"; key = "login.error"; }
        if (status.value() == 401) headers.set(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        if (failure instanceof PasswordLoginLockedException locked) {
            headers.set("Retry-After", Long.toString(locked.getRetryAfterSeconds()));
            code = "login.locked"; key = "login.locked";
        }
        headers.setCacheControl("no-store"); headers.setContentType(MediaType.APPLICATION_PROBLEM_JSON);
        var problem = problems.create(status, code, key, request);
        if (failure instanceof BindException binding) {
            var fields = new LinkedHashMap<String, String>();
            binding.getFieldErrors().forEach(error -> fields.putIfAbsent(error.getField(), "invalid"));
            problem.setProperty("fields", fields);
        }
        if (status.is5xxServerError()) log.error("Unexpected API failure", SafeFailure.snapshot(failure));
        else log.debug("API request rejected (status={}, code={})", status.value(), code);
        return new ResponseEntity<>(problem, headers, status);
    }
}
