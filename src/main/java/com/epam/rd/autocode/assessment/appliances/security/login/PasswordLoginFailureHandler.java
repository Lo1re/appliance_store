package com.epam.rd.autocode.assessment.appliances.security.login;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

/** MVC presentation only: both known and unknown accounts receive the same messages. */
@Component
public class PasswordLoginFailureHandler implements AuthenticationFailureHandler {
    private final AuthenticationFailureHandler invalid = new SimpleUrlAuthenticationFailureHandler("/login?error");
    private final AuthenticationFailureHandler locked = new SimpleUrlAuthenticationFailureHandler("/login?locked");

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException failure) throws IOException, ServletException {
        (failure instanceof LockedException ? locked : invalid).onAuthenticationFailure(request, response, failure);
    }
}
