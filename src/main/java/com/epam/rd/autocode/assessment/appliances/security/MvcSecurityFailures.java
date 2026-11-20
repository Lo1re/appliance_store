package com.epam.rd.autocode.assessment.appliances.security;

import com.epam.rd.autocode.assessment.appliances.web.error.MvcRouteClassifier;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class MvcSecurityFailures implements AuthenticationEntryPoint, AccessDeniedHandler {
    private final MvcRouteClassifier routes;
    private final LoginUrlAuthenticationEntryPoint login = new LoginUrlAuthenticationEntryPoint("/login");

    public MvcSecurityFailures(MvcRouteClassifier routes) {
        this.routes = routes;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException exception) throws IOException, ServletException {
        if (routes.isUnmappedRead(request)) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        } else {
            login.commence(request, response, exception);
        }
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException exception) throws IOException {
        int status = routes.isUnmappedRead(request)
                ? HttpServletResponse.SC_NOT_FOUND : HttpServletResponse.SC_FORBIDDEN;
        if (status == HttpServletResponse.SC_FORBIDDEN) {
            log.warn("Authorization rejected (status={}, accountId={})", status, SecurityLogContext.currentAccountId());
        }
        response.sendError(status);
    }

}
