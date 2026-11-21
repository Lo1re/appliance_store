package com.epam.rd.autocode.assessment.appliances.security.oauth.google.web;

import com.epam.rd.autocode.assessment.appliances.security.oauth.google.flow.GoogleFlowSession;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;

@RequiredArgsConstructor @Slf4j
public class GoogleLoginFailureHandler implements AuthenticationFailureHandler {
    private final GoogleFlowSession flows;
    @Override public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException {
        flows.finish(request, response);
        response.setHeader("Cache-Control", "no-store");
        log.warn("Google authorization rejected");
        response.sendRedirect(request.getContextPath() + "/login?googleError");
    }
}
