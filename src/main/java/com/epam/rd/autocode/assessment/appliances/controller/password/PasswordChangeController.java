package com.epam.rd.autocode.assessment.appliances.controller.password;

import com.epam.rd.autocode.assessment.appliances.dto.password.PasswordChangeRequest;
import com.epam.rd.autocode.assessment.appliances.exception.password.PasswordChangeError;
import com.epam.rd.autocode.assessment.appliances.exception.password.PasswordChangeException;
import com.epam.rd.autocode.assessment.appliances.logging.SafeFailure;
import com.epam.rd.autocode.assessment.appliances.security.identity.CurrentActor;
import com.epam.rd.autocode.assessment.appliances.security.oauth.google.identity.GoogleIdentityService;
import com.epam.rd.autocode.assessment.appliances.security.password.PasswordCredentialStatus;
import com.epam.rd.autocode.assessment.appliances.security.password.PasswordCredentialStatusService;
import com.epam.rd.autocode.assessment.appliances.security.password.reset.PasswordResetService;
import com.epam.rd.autocode.assessment.appliances.security.web.PasswordChangeLogoutHandler;
import com.epam.rd.autocode.assessment.appliances.service.password.PasswordChangeService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.LockedException;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.TransactionException;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Locale;

@Controller
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "store.security.auth-mode",
        havingValue = "database",
        matchIfMissing = true)
public class PasswordChangeController {
    private static final String PAGE = "password/change";
    private final PasswordChangeService passwords;
    private final PasswordChangeLogoutHandler logout;
    private final PasswordCredentialStatusService passwordState;
    private final CurrentActor actor;
    private final GoogleIdentityService google;
    private final ObjectProvider<PasswordResetService> recovery;

    @ModelAttribute
    public void accountContext(Model model, HttpServletRequest request) {
        var account = actor.requireUser();
        model.addAttribute("googleLinked", google.linked(account.getId()));
        var status = passwordState.status(account.getId());
        boolean legacyChange =
                status == PasswordCredentialStatus.UNKNOWN
                        && ("change".equals(request.getParameter("mode"))
                                || ("POST".equals(request.getMethod())
                                        && request.getRequestURI().endsWith("/password/change")));
        model.addAttribute(
                "showPasswordChangeForm", status == PasswordCredentialStatus.SET || legacyChange);
        model.addAttribute("passwordStateUnknown", status == PasswordCredentialStatus.UNKNOWN);
        model.addAttribute("passwordAccountEmail", account.getEmail());
    }

    @GetMapping("/password/change")
    public String form(Model model, HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
        model.addAttribute("passwordForm", new PasswordChangeRequest());
        return PAGE;
    }

    @PostMapping("/password/setup")
    public String requestSetup(
            HttpServletRequest request,
            HttpServletResponse response,
            Model model,
            Locale locale,
            RedirectAttributes redirects) {
        response.setHeader("Cache-Control", "no-store");
        var reset = recovery.getIfAvailable();
        if (reset == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        try {
            long retry =
                    reset.request(actor.requireUser().getEmail(), request.getRemoteAddr(), locale);
            if (retry > 0) {
                response.setStatus(429);
                response.setHeader("Retry-After", Long.toString(retry));
                model.addAttribute("retryAfterSeconds", retry);
                model.addAttribute("passwordForm", new PasswordChangeRequest());
                return PAGE;
            }
            return "redirect:/password/change?setupSent";
        } catch (DataAccessException | TransactionException unavailable) {
            throw new AuthenticationServiceException(
                    "Password setup temporarily unavailable", SafeFailure.snapshot(unavailable));
        }
    }

    @PostMapping("/password/change")
    public String change(
            @Valid @ModelAttribute("passwordForm") PasswordChangeRequest form,
            BindingResult errors,
            HttpServletRequest request,
            HttpServletResponse response,
            RedirectAttributes redirects) {
        response.setHeader("Cache-Control", "no-store");
        if (errors.hasErrors()) {
            response.setStatus(400);
            return PAGE;
        }
        try {
            passwords.change(form);
        } catch (PasswordChangeException rejected) {
            if (rejected.error() == PasswordChangeError.NOT_SET) {
                return "redirect:/password/change";
            }
            String field =
                    rejected.error() == PasswordChangeError.CURRENT_INVALID
                            ? "currentPassword"
                            : "newPassword";
            errors.rejectValue(field, rejected.error().messageKey());
            response.setStatus(400);
            return PAGE;
        } catch (LockedException locked) {
            errors.reject("login.locked");
            response.setStatus(429);
            return PAGE;
        } catch (DataAccessException | TransactionException unavailable) {
            throw new AuthenticationServiceException(
                    "Password change temporarily unavailable", SafeFailure.snapshot(unavailable));
        }
        logout.logout(request, response);
        return "redirect:/login?passwordChanged";
    }
}
