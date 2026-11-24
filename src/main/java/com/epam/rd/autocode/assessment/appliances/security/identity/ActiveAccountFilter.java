package com.epam.rd.autocode.assessment.appliances.security.identity;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.RequiredArgsConstructor;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

// Clears stale session authentication before authorization uses its cached authorities.
@RequiredArgsConstructor
public class ActiveAccountFilter extends OncePerRequestFilter {
    private final CurrentActor currentActor;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AccountPrincipal) {
            try {
                currentActor.requireUser();
            } catch (AccessDeniedException staleAccount) {
                new SecurityContextLogoutHandler().logout(request, response, authentication);
            }
        }
        chain.doFilter(request, response);
    }
}
