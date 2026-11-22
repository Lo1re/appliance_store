package com.epam.rd.autocode.assessment.appliances.controller.oauth;

import com.epam.rd.autocode.assessment.appliances.dto.oauth.GoogleProfileRequest;
import com.epam.rd.autocode.assessment.appliances.exception.AccountException;
import com.epam.rd.autocode.assessment.appliances.logging.SafeFailure;
import com.epam.rd.autocode.assessment.appliances.security.jwt.cookie.AuthTokenCookies;
import com.epam.rd.autocode.assessment.appliances.security.jwt.cookie.JwtCookieLifecycle;
import com.epam.rd.autocode.assessment.appliances.security.jwt.refresh.RefreshTokenService;
import com.epam.rd.autocode.assessment.appliances.security.jwt.web.JwtLoginSuccessHandler;
import com.epam.rd.autocode.assessment.appliances.security.oauth.google.config.ConditionalOnGoogleLogin;
import com.epam.rd.autocode.assessment.appliances.security.oauth.google.flow.GoogleEmailVerification;
import com.epam.rd.autocode.assessment.appliances.security.oauth.google.flow.GoogleFlowSession;
import com.epam.rd.autocode.assessment.appliances.security.oauth.google.identity.GoogleAccountAuthentication;
import com.epam.rd.autocode.assessment.appliances.security.oauth.google.identity.GoogleIdentityService;
import com.epam.rd.autocode.assessment.appliances.security.oauth.google.identity.GoogleLoginException;
import com.epam.rd.autocode.assessment.appliances.security.oauth.google.identity.VerifiedGoogleIdentity;
import com.epam.rd.autocode.assessment.appliances.web.form.AccountFormView;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.TransactionException;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller @RequiredArgsConstructor @ConditionalOnGoogleLogin
public class GoogleOnboardingController {
    private final GoogleFlowSession flows;
    private final GoogleEmailVerification verification;
    private final GoogleIdentityService identities;
    private final GoogleAccountAuthentication accounts;
    private final RefreshTokenService tokens;
    private final JwtCookieLifecycle cookies;
    private final AuthTokenCookies browserCookies;
    private final AccountFormView forms;

    @ModelAttribute public void noCache(HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store"); response.setHeader("Referrer-Policy", "no-referrer");
    }
    @GetMapping("/onboarding/google")
    public String form(HttpServletRequest request, HttpServletResponse response, Model model, RedirectAttributes redirects) {
        try {
            var identity = flows.requirePending(request);
            var form = flows.draft(request).orElseGet(() -> {
                var initial = new GoogleProfileRequest(); initial.setName(identity.name()); initial.setEmail(identity.email()); return initial;
            });
            model.addAttribute("googleProfile", form);
            return "oauth/google-profile";
        } catch (GoogleLoginException expired) {
            flows.finish(request, response); return "redirect:/login?googleExpired";
        }
    }
    @PostMapping("/onboarding/google")
    public String complete(@Valid @ModelAttribute("googleProfile") GoogleProfileRequest form, BindingResult errors,
            HttpServletRequest request, HttpServletResponse response, Model model, Locale locale, RedirectAttributes redirects) throws IOException {
        try {
            var identity = flows.requirePending(request);
            if (errors.hasErrors()) { response.setStatus(400); return "oauth/google-profile"; }
            flows.saveDraft(request, form);
            if (!flows.emailVerified(request, form.getEmail())) {
                long retry = verification.send(request, form.getEmail(), locale);
                if (retry > 0) {
                    response.setStatus(429); response.setHeader("Retry-After", Long.toString(retry));
                    model.addAttribute("retryAfterSeconds", retry);
                } else model.addAttribute("verificationSent", true);
                return "oauth/google-profile";
            }
            var verified = new VerifiedGoogleIdentity(identity.subject(), form.getEmail(), identity.name(), true);
            Long id = identities.createProfile(verified, form);
            var authentication = accounts.forAccount(id);
            flows.finish(request, response);
            new JwtLoginSuccessHandler(tokens, cookies, browserCookies).onAuthenticationSuccess(request, response, authentication);
            return null;
        } catch (GoogleLoginException rejected) {
            flows.finish(request, response);
            return rejected.reason() == GoogleLoginException.Reason.LINK_REQUIRED ? "redirect:/login?googleLinkRequired" : "redirect:/login?googleExpired";
        } catch (AccountException conflict) {
            forms.rejectDuplicate(conflict, errors); response.setStatus(409); return "oauth/google-profile";
        } catch (DataIntegrityViolationException conflict) {
            errors.reject("google.profile.conflict"); response.setStatus(409); return "oauth/google-profile";
        } catch (DataAccessException | TransactionException unavailable) {
            throw new AuthenticationServiceException("Google onboarding temporarily unavailable", SafeFailure.snapshot(unavailable));
        }
    }
    @GetMapping("/onboarding/google/verify")
    public String verify(@RequestParam(defaultValue = "") String token, HttpServletRequest request, HttpServletResponse response, RedirectAttributes redirects) {
        try { verification.confirm(request, token); return "redirect:/onboarding/google?verified"; }
        catch (GoogleLoginException expired) { flows.finish(request, response); return "redirect:/login?googleExpired"; }
    }
    @PostMapping("/onboarding/google/cancel")
    public String cancel(HttpServletRequest request, HttpServletResponse response, RedirectAttributes redirects) {
        flows.finish(request, response); return "redirect:/login";
    }
}
