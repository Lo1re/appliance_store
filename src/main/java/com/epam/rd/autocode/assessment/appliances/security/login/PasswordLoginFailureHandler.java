package com.epam.rd.autocode.assessment.appliances.security.login;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

/** Generic localized redirects without saving exceptions or credentials in a session. */
@Component
public class PasswordLoginFailureHandler implements AuthenticationFailureHandler {
    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException failure)
            throws IOException {
        response.sendRedirect(request.getContextPath() + (failure instanceof LockedException ? "/login?locked" : "/login?error"));
    }
}
