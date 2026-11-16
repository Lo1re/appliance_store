package com.epam.rd.autocode.assessment.appliances.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

@Slf4j
@Component
public class MvcSecurityFailures implements AuthenticationEntryPoint, AccessDeniedHandler {
    private final RequestMappingHandlerMapping routes;
    private final LoginUrlAuthenticationEntryPoint login = new LoginUrlAuthenticationEntryPoint("/login");

    public MvcSecurityFailures(
            @Qualifier("requestMappingHandlerMapping") RequestMappingHandlerMapping routes) {
        this.routes = routes;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException exception) throws IOException, ServletException {
        if (isUnmappedRead(request)) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        } else {
            login.commence(request, response, exception);
        }
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException exception) throws IOException {
        int status = isUnmappedRead(request)
                ? HttpServletResponse.SC_NOT_FOUND : HttpServletResponse.SC_FORBIDDEN;
        if (status == HttpServletResponse.SC_FORBIDDEN) {
            log.warn("Authorization rejected (status={}, accountId={})", status, SecurityLogContext.currentAccountId());
        }
        response.sendError(status);
    }

    private boolean isUnmappedRead(HttpServletRequest request) {
        if (!"GET".equals(request.getMethod()) && !"HEAD".equals(request.getMethod())) {
            return false;
        }
        try {
            return routes.getHandler(request) == null;
        } catch (Exception exception) {
            log.debug("Could not classify rejected route (type={})", exception.getClass().getSimpleName());
            return false;
        }
    }
}
