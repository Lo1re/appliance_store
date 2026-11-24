package com.epam.rd.autocode.assessment.appliances.security.jwt.web;

import com.epam.rd.autocode.assessment.appliances.logging.SafeFailure;
import com.epam.rd.autocode.assessment.appliances.security.identity.CurrentActor;
import com.epam.rd.autocode.assessment.appliances.security.jwt.access.AccessTokenAuthentication;
import com.epam.rd.autocode.assessment.appliances.security.jwt.cookie.AuthTokenCookies;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.dao.DataAccessException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.transaction.TransactionException;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

// Rechecks account existence and current roles before URL authorization uses JWT claims.
@Slf4j
@RequiredArgsConstructor
public final class JwtAccountFilter extends OncePerRequestFilter {
    private final CurrentActor actor;
    private final AuthTokenCookies cookies;
    private final AuthenticationEntryPoint failures;
    private final boolean clearCookies;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof AccessTokenAuthentication) {
            try {
                actor.requireUser();
            } catch (DataAccessException | TransactionException unavailable) {
                log.error(
                        "Account authentication storage unavailable",
                        SafeFailure.snapshot(unavailable));
                SecurityContextHolder.clearContext();
                failures.commence(
                        request,
                        response,
                        new AuthenticationServiceException(
                                "Authentication temporarily unavailable",
                                SafeFailure.snapshot(unavailable)));
                return; // Keep browser credentials; an outage does not make them invalid.
            } catch (AccessDeniedException stale) {
                SecurityContextHolder.clearContext();
                if (clearCookies) {
                    cookies.clear(request, response);
                }
                failures.commence(
                        request,
                        response,
                        new CredentialsExpiredException("Account credentials are no longer valid"));
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
