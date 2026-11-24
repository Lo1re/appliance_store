package com.epam.rd.autocode.assessment.appliances.logging;

import com.epam.rd.autocode.assessment.appliances.security.logging.SecurityLogContext;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerMapping;

import java.util.Locale;
import java.util.Set;

public record HttpLogMetadata(
        String method,
        String route,
        String path,
        String handler,
        String clientIp,
        long requestBytes) {
    private static final Set<String> SENSITIVE_SEGMENTS =
            Set.of(
                    "auth",
                    "login",
                    "logout",
                    "password",
                    "passwords",
                    "reset",
                    "reset-password",
                    "forgot-password",
                    "verify",
                    "verification",
                    "verify-email",
                    "token",
                    "tokens",
                    "refresh",
                    "oauth",
                    "oauth2",
                    "callback",
                    "onboarding",
                    "linking");

    public static HttpLogMetadata capture(HttpServletRequest request) {
        String method = request.getMethod();
        method = method != null && method.matches("[A-Z]{1,16}") ? method : "OTHER";
        Object pattern = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        String route = pattern == null ? "-" : safePattern(pattern.toString());
        String path =
                !route.equals("-") && !route.contains("*")
                        ? route
                        : safePath(request.getRequestURI());
        Object mapped = request.getAttribute(HandlerMapping.BEST_MATCHING_HANDLER_ATTRIBUTE);
        String handler =
                mapped instanceof HandlerMethod methodHandler
                        ? methodHandler.getBeanType().getSimpleName()
                                + "."
                                + methodHandler.getMethod().getName()
                        : "-";
        return new HttpLogMetadata(
                method,
                route,
                path,
                handler,
                SecurityLogContext.safeRemoteAddress(request.getRemoteAddr()),
                request.getContentLengthLong());
    }

    private static String safePattern(String route) {
        return route.length() <= 256
                        && !route.contains("\r")
                        && !route.contains("\n")
                        && !route.contains("?")
                        && !route.contains("#")
                ? route
                : "-";
    }

    private static String safePath(String uri) {
        if (uri == null || !uri.startsWith("/") || uri.length() > 256) {
            return "/[redacted]";
        }
        int query = uri.indexOf('?');
        if (query >= 0) {
            uri = uri.substring(0, query);
        }
        int fragment = uri.indexOf('#');
        if (fragment >= 0) {
            uri = uri.substring(0, fragment);
        }
        var path = new StringBuilder();
        boolean sensitive = false;
        for (String segment : uri.substring(1).split("/", -1)) {
            path.append('/');
            if (sensitive) {
                path.append("[redacted]");
                break;
            }
            int matrix = segment.indexOf(';');
            if (matrix >= 0) {
                segment = segment.substring(0, matrix);
            }
            path.append(
                    segment.isEmpty() || segment.matches("[A-Za-z0-9._~-]{1,24}")
                            ? segment
                            : "[redacted]");
            sensitive = SENSITIVE_SEGMENTS.contains(segment.toLowerCase(Locale.ROOT));
        }
        return path.toString();
    }
}
