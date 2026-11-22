package com.epam.rd.autocode.assessment.appliances.security.oauth.google.flow;

import com.epam.rd.autocode.assessment.appliances.security.oauth.google.identity.GoogleLoginException;
import com.epam.rd.autocode.assessment.appliances.security.oauth.google.identity.VerifiedGoogleIdentity;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.CookieClearingLogoutHandler;
import org.springframework.stereotype.Component;

/** Temporary OIDC workflow only. It never stores an authenticated shop SecurityContext. */
@Component @RequiredArgsConstructor
public class GoogleFlowSession {
    private static final String PENDING = GoogleFlowSession.class.getName() + ".pending";
    private static final String VERIFIED_EMAIL = GoogleFlowSession.class.getName() + ".verifiedEmail";
    private static final String CHALLENGE = GoogleFlowSession.class.getName() + ".challenge";
    private static final String DRAFT = GoogleFlowSession.class.getName() + ".draft";
    private static final String LINK = GoogleFlowSession.class.getName() + ".link";
    private final Clock clock;

    public void pending(HttpServletRequest request, VerifiedGoogleIdentity identity) {
        var session = request.getSession();
        request.changeSessionId();
        session.setMaxInactiveInterval(900);
        session.removeAttribute(LINK);
        session.removeAttribute(CHALLENGE);
        session.removeAttribute(DRAFT);
        session.setAttribute(VERIFIED_EMAIL, identity.emailVerified() ? identity.email() : "");
        session.setAttribute(PENDING, new Pending(identity, clock.instant().plusSeconds(900)));
        SecurityContextHolder.clearContext();
    }
    public VerifiedGoogleIdentity requirePending(HttpServletRequest request) {
        var session = request.getSession(false);
        if (session != null && session.getAttribute(PENDING) instanceof Pending pending && pending.until().isAfter(clock.instant()))
            return pending.identity();
        throw expired();
    }
    public boolean emailVerified(HttpServletRequest request, String email) {
        requirePending(request);
        return email != null && email.equals(request.getSession(false).getAttribute(VERIFIED_EMAIL));
    }
    public void saveDraft(HttpServletRequest request, com.epam.rd.autocode.assessment.appliances.dto.oauth.GoogleProfileRequest form) {
        requirePending(request);
        var copy = new com.epam.rd.autocode.assessment.appliances.dto.oauth.GoogleProfileRequest();
        copy.setName(form.getName()); copy.setEmail(form.getEmail()); copy.setCard(form.getCard());
        request.getSession(false).setAttribute(DRAFT, copy);
    }
    public Optional<com.epam.rd.autocode.assessment.appliances.dto.oauth.GoogleProfileRequest> draft(HttpServletRequest request) {
        requirePending(request);
        Object value = request.getSession(false).getAttribute(DRAFT);
        return value instanceof com.epam.rd.autocode.assessment.appliances.dto.oauth.GoogleProfileRequest form ? Optional.of(form) : Optional.empty();
    }
    public void challenge(HttpServletRequest request, String email, String tokenHash) {
        requirePending(request);
        request.getSession(false).setAttribute(CHALLENGE, new Challenge(email, tokenHash, clock.instant().plusSeconds(600)));
    }
    public void verifyEmail(HttpServletRequest request, String tokenHash) {
        requirePending(request);
        var session = request.getSession(false);
        if (!(session.getAttribute(CHALLENGE) instanceof Challenge challenge) || !challenge.until().isAfter(clock.instant())
                || !java.security.MessageDigest.isEqual(challenge.hash().getBytes(java.nio.charset.StandardCharsets.US_ASCII),
                        tokenHash.getBytes(java.nio.charset.StandardCharsets.US_ASCII))) throw expired();
        session.setAttribute(VERIFIED_EMAIL, challenge.email());
        session.removeAttribute(CHALLENGE);
    }
    private record Challenge(String email, String hash, Instant until) {
        @Override public String toString() { return "GoogleEmailChallenge[redacted]"; }
    }

    public void beginLink(HttpServletRequest request, Long accountId, String accessHash) {
        var session = request.getSession();
        request.changeSessionId();
        session.setMaxInactiveInterval(900);
        session.removeAttribute(PENDING);
        session.setAttribute(LINK, new LinkIntent(accountId, accessHash, clock.instant().plusSeconds(900)));
    }
    public Optional<LinkIntent> link(HttpServletRequest request) {
        var session = request.getSession(false);
        if (session == null || session.getAttribute(LINK) == null) {
            return Optional.empty();
        }
        if (session.getAttribute(LINK) instanceof LinkIntent intent && intent.until().isAfter(clock.instant())) {
            return Optional.of(intent);
        }
        throw expired();
    }
    public void finish(HttpServletRequest request, HttpServletResponse response) {
        var session = request.getSession(false);
        if (session != null) session.invalidate();
        SecurityContextHolder.clearContext();
        new CookieClearingLogoutHandler("JSESSIONID").logout(request, response, null);
    }
    private GoogleLoginException expired() { return new GoogleLoginException(GoogleLoginException.Reason.FLOW_EXPIRED); }
    private record Pending(VerifiedGoogleIdentity identity, Instant until) {
        @Override public String toString() { return "GooglePendingProfile[redacted]"; }
    }
    public record LinkIntent(Long accountId, String accessHash, Instant until) {
        @Override public String toString() { return "GoogleLinkIntent[redacted]"; }
    }
}
