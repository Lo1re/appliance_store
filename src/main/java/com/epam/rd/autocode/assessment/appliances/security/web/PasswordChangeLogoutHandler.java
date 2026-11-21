package com.epam.rd.autocode.assessment.appliances.security.web;

import com.epam.rd.autocode.assessment.appliances.security.jwt.cookie.JwtCookieLifecycle;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.CookieClearingLogoutHandler;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Component;

/** Browser cleanup only, after the password service has committed. */
@Component
@RequiredArgsConstructor
public class PasswordChangeLogoutHandler {
    private final ObjectProvider<JwtCookieLifecycle> cookies;
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        var lifecycle = cookies.getIfAvailable();
        if (lifecycle != null) lifecycle.clear(request, response);
        new SecurityContextLogoutHandler().logout(request, response, authentication);
        new CookieClearingLogoutHandler("JSESSIONID").logout(request, response, authentication);
    }
}
