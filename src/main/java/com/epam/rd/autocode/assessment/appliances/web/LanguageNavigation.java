package com.epam.rd.autocode.assessment.appliances.web;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.stereotype.Component;

/** Prepares language links, retaining only non-sensitive list filters. */
@Component
public class LanguageNavigation {
    private static final List<String> FILTERS = List.of("search", "sort", "page", "status", "error", "logout", "expired");

    public Links links(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (request.getDispatcherType() == DispatcherType.ERROR || path == null
                || !path.startsWith("/") || path.startsWith("//") || path.contains("\r") || path.contains("\n")) {
            path = request.getContextPath() + "/";
        }
        return new Links(link(path, "uk", request), link(path, "en", request));
    }

    private String link(String path, String language, HttpServletRequest request) {
        var result = new StringBuilder(path).append("?lang=").append(language);
        for (String key : FILTERS) {
            String value = request.getParameter(key);
            if (value != null && value.length() <= 100) {
                result.append('&').append(key).append('=').append(URLEncoder.encode(value, StandardCharsets.UTF_8));
            }
        }
        return result.toString();
    }

    public record Links(String uk, String en) { }
}
