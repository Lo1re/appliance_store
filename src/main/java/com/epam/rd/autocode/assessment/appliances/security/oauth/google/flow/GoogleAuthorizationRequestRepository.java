package com.epam.rd.autocode.assessment.appliances.security.oauth.google.flow;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.client.web.HttpSessionOAuth2AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

import java.time.Clock;
import java.time.Instant;

// Standard state/nonce/PKCE storage with a hard 15-minute authorization lifetime.
public class GoogleAuthorizationRequestRepository
        implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {
    private static final String UNTIL =
            GoogleAuthorizationRequestRepository.class.getName() + ".until";
    private final Clock clock;
    private final HttpSessionOAuth2AuthorizationRequestRepository delegate =
            new HttpSessionOAuth2AuthorizationRequestRepository();

    public GoogleAuthorizationRequestRepository(Clock clock) {
        this.clock = clock;
    }

    @Override
    public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
        return valid(request) ? delegate.loadAuthorizationRequest(request) : null;
    }

    @Override
    public void saveAuthorizationRequest(
            OAuth2AuthorizationRequest authorization,
            HttpServletRequest request,
            HttpServletResponse response) {
        delegate.saveAuthorizationRequest(authorization, request, response);
        var session = request.getSession(false);
        if (session != null) {
            if (authorization == null) {
                session.removeAttribute(UNTIL);
            } else {
                session.setMaxInactiveInterval(900);
                session.setAttribute(UNTIL, clock.instant().plusSeconds(900));
            }
        }
    }

    @Override
    public OAuth2AuthorizationRequest removeAuthorizationRequest(
            HttpServletRequest request, HttpServletResponse response) {
        boolean valid = valid(request);
        var authorization = delegate.removeAuthorizationRequest(request, response);
        var session = request.getSession(false);
        if (session != null) {
            session.removeAttribute(UNTIL);
        }
        return valid ? authorization : null;
    }

    private boolean valid(HttpServletRequest request) {
        var session = request.getSession(false);
        return session != null
                && session.getAttribute(UNTIL) instanceof Instant until
                && until.isAfter(clock.instant());
    }
}
