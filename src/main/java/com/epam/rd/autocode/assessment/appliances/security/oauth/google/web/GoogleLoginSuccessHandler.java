package com.epam.rd.autocode.assessment.appliances.security.oauth.google.web;

import com.epam.rd.autocode.assessment.appliances.logging.SafeFailure;
import com.epam.rd.autocode.assessment.appliances.security.jwt.cookie.AuthTokenCookies;
import com.epam.rd.autocode.assessment.appliances.security.jwt.cookie.JwtCookieLifecycle;
import com.epam.rd.autocode.assessment.appliances.security.jwt.refresh.RefreshTokenService;
import com.epam.rd.autocode.assessment.appliances.security.jwt.web.JwtLoginSuccessHandler;
import com.epam.rd.autocode.assessment.appliances.security.oauth.google.flow.GoogleFlowSession;
import com.epam.rd.autocode.assessment.appliances.security.oauth.google.flow.GoogleLinkVerifier;
import com.epam.rd.autocode.assessment.appliances.security.oauth.google.identity.GoogleAccountAuthentication;
import com.epam.rd.autocode.assessment.appliances.security.oauth.google.identity.GoogleIdentityService;
import com.epam.rd.autocode.assessment.appliances.security.oauth.google.identity.GoogleIdentityVerifier;
import com.epam.rd.autocode.assessment.appliances.security.oauth.google.identity.GoogleLoginException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.transaction.TransactionException;

@RequiredArgsConstructor @Slf4j
public class GoogleLoginSuccessHandler implements AuthenticationSuccessHandler {
    private final GoogleIdentityVerifier verifier;
    private final GoogleIdentityService identities;
    private final GoogleAccountAuthentication accounts;
    private final GoogleFlowSession flows;
    private final RefreshTokenService tokens;
    private final JwtCookieLifecycle cookies;
    private final AuthTokenCookies browserCookies;
    private final GoogleLinkVerifier links;

    @Override public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
        response.setHeader("Cache-Control", "no-store");
        try {
            if (!(authentication instanceof OAuth2AuthenticationToken oauth) || !"google".equals(oauth.getAuthorizedClientRegistrationId())
                    || !(oauth.getPrincipal() instanceof OidcUser oidc))
                throw new GoogleLoginException(GoogleLoginException.Reason.INVALID_IDENTITY);
            var identity = verifier.verify(oidc);
            var intent = flows.link(request);
            if (intent.isPresent()) {
                var link = intent.orElseThrow();
                links.verify(request, link);
                identities.link(link.accountId(), identity);
                flows.finish(request, response);
                log.info("Google identity linked (accountId={})", link.accountId());
                response.sendRedirect(request.getContextPath() + "/account/google?linked");
                return;
            }
            var account = identities.findAccount(identity.subject());
            if (account.isPresent()) {
                var local = accounts.forAccount(account.orElseThrow());
                flows.finish(request, response);
                new JwtLoginSuccessHandler(tokens, cookies, browserCookies).onAuthenticationSuccess(request, response, local);
                log.info("Google sign-in completed (accountId={})", account.orElseThrow());
            } else {
                if (identity.emailVerified() && identities.emailTaken(identity.email())) {
                    throw new GoogleLoginException(GoogleLoginException.Reason.LINK_REQUIRED);
                }
                flows.pending(request, identity);
                response.sendRedirect(request.getContextPath() + "/onboarding/google");
            }
        } catch (GoogleLoginException rejected) {
            flows.finish(request, response);
            String flag = rejected.reason() == GoogleLoginException.Reason.LINK_REQUIRED ? "googleLinkRequired" : "googleError";
            response.sendRedirect(request.getContextPath() + "/login?" + flag);
        } catch (DataIntegrityViolationException conflict) {
            flows.finish(request, response);
            response.sendRedirect(request.getContextPath() + "/login?googleError");
        } catch (DataAccessException | TransactionException | AuthenticationServiceException unavailable) {
            flows.finish(request, response);
            log.error("Google sign-in storage unavailable", SafeFailure.snapshot(unavailable));
            response.sendError(503);
        } catch (RuntimeException invalid) {
            flows.finish(request, response);
            log.warn("Google sign-in rejected");
            response.sendRedirect(request.getContextPath() + "/login?googleError");
        }
    }
}
