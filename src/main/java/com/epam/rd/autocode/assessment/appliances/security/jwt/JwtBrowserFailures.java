package com.epam.rd.autocode.assessment.appliances.security.jwt;

import com.epam.rd.autocode.assessment.appliances.security.MvcSecurityFailures;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

/** Clears rejected credentials, then preserves the existing HTML 404/login policy. */
@RequiredArgsConstructor
public final class JwtBrowserFailures implements AuthenticationEntryPoint {
    private final AccessTokenCookies cookies;
    private final MvcSecurityFailures failures;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException failure)
            throws IOException, ServletException {
        cookies.clear(request, response);
        failures.commence(request, response, failure);
    }
}
