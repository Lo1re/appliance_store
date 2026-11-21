package com.epam.rd.autocode.assessment.appliances.security.jwt;

import com.epam.rd.autocode.assessment.appliances.security.CurrentActor;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.web.filter.OncePerRequestFilter;

/** Rechecks account existence and current roles before URL authorization uses JWT claims. */
@RequiredArgsConstructor
public final class JwtAccountFilter extends OncePerRequestFilter {
    private final CurrentActor actor;
    private final AccessTokenCookies cookies;
    private final AuthenticationEntryPoint failures;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof AccessTokenAuthentication) {
            try { actor.requireUser(); }
            catch (AccessDeniedException stale) {
                SecurityContextHolder.clearContext(); cookies.clear(request, response);
                failures.commence(request, response, new InsufficientAuthenticationException("Account credentials are no longer valid"));
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
