package com.epam.rd.autocode.assessment.appliances.controller;

import com.epam.rd.autocode.assessment.appliances.logging.SafeFailure;
import com.epam.rd.autocode.assessment.appliances.web.error.MvcErrorViews;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.TypeMismatchException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.validation.BindException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

@Slf4j
@Order(Ordered.LOWEST_PRECEDENCE)
@ControllerAdvice
public class MvcExceptionAdvice {
    @ExceptionHandler(Exception.class)
    public ModelAndView handleFailure(Exception exception, HttpServletResponse response) {
        HttpStatusCode status;
        if (exception instanceof AuthenticationServiceException) {
            status = HttpStatus.SERVICE_UNAVAILABLE;
        } else if (exception instanceof AccessDeniedException) {
            status = HttpStatus.FORBIDDEN;
        } else if (exception instanceof TypeMismatchException
                || exception instanceof BindException
                || exception instanceof ConstraintViolationException) {
            status = HttpStatus.BAD_REQUEST;
        } else if (exception instanceof ErrorResponse error) {
            status = error.getStatusCode();
            error.getHeaders().forEach((name, values) ->
                    values.forEach(value -> response.addHeader(name, value)));
        } else {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }

        if (status.is5xxServerError()) {
            log.error("Unexpected MVC failure (type={})", exception.getClass().getSimpleName(), SafeFailure.snapshot(exception));
        } else {
            log.debug("MVC request rejected (status={}, type={})", status.value(),
                    exception.getClass().getSimpleName());
        }
        return MvcErrorViews.render(status);
    }
}
