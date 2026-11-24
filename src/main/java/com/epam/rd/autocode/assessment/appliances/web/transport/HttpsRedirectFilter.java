package com.epam.rd.autocode.assessment.appliances.web.transport;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpHeaders;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.URI;

// Redirects reads to a configured trusted origin before authentication; never replays HTTP writes.
public final class HttpsRedirectFilter extends OncePerRequestFilter {
    private final String origin;

    public HttpsRedirectFilter(String publicOrigin) {
        URI uri = URI.create(publicOrigin);
        if (!"https".equals(uri.getScheme())
                || uri.getHost() == null
                || uri.getUserInfo() != null
                || uri.getRawQuery() != null
                || uri.getRawFragment() != null
                || !(uri.getPath().isEmpty() || "/".equals(uri.getPath()))) {
            throw new IllegalArgumentException(
                    "HTTPS redirect requires a trusted HTTPS origin without a path");
        }
        origin = publicOrigin.replaceAll("/+$", "");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (request.isSecure()) {
            chain.doFilter(request, response);
            return;
        }
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        if (!"GET".equals(request.getMethod()) && !"HEAD".equals(request.getMethod())) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setContentType("text/plain;charset=UTF-8");
            response.getWriter().write("Use HTTPS to submit data.");
            return;
        }
        String path = request.getRequestURI();
        String query = request.getQueryString();
        response.setStatus(308);
        response.setHeader("Location", origin + path + (query == null ? "" : "?" + query));
    }
}
