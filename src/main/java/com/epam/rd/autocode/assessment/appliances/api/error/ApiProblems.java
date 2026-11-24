package com.epam.rd.autocode.assessment.appliances.api.error;

import com.epam.rd.autocode.assessment.appliances.logging.RequestLogContext;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.MessageSource;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

import java.util.List;
import java.util.Locale;

// Creates safe localized API bodies; credentials and exception messages are never copied.
@Component
@ConditionalOnProperty(name = "store.api.enabled", havingValue = "true")
public class ApiProblems {
    private final MessageSource messages;
    private final AcceptHeaderLocaleResolver locales = new AcceptHeaderLocaleResolver();

    public ApiProblems(MessageSource messages) {
        this.messages = messages;
        locales.setSupportedLocales(List.of(Locale.ENGLISH, Locale.forLanguageTag("uk")));
        locales.setDefaultLocale(Locale.ENGLISH);
    }

    public ProblemDetail create(
            HttpStatusCode status, String code, String detailKey, HttpServletRequest request) {
        Locale locale = locales.resolveLocale(request);
        String titleKey = "error." + status.value() + ".title";
        String fallbackTitle = status.is5xxServerError() ? "error.5xx.title" : "error.4xx.title";
        String fallbackDetail =
                status.is5xxServerError() ? "error.5xx.message" : "error.4xx.message";
        var problem =
                ProblemDetail.forStatusAndDetail(
                        status, message(detailKey, fallbackDetail, locale));
        problem.setTitle(message(titleKey, fallbackTitle, locale));
        problem.setProperty("code", code);
        Object requestId = request.getAttribute(RequestLogContext.ATTRIBUTE);
        if (requestId != null) {
            problem.setProperty("requestId", requestId.toString());
        }
        return problem;
    }

    private String message(String key, String fallbackKey, Locale locale) {
        return messages.getMessage(
                new DefaultMessageSourceResolvable(new String[] {key, fallbackKey}), locale);
    }
}
