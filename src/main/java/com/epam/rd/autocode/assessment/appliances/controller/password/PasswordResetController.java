package com.epam.rd.autocode.assessment.appliances.controller.password;

import com.epam.rd.autocode.assessment.appliances.dto.password.ForgotPasswordRequest;
import com.epam.rd.autocode.assessment.appliances.dto.password.PasswordResetRequest;
import com.epam.rd.autocode.assessment.appliances.exception.password.PasswordChangeException;
import com.epam.rd.autocode.assessment.appliances.logging.SafeFailure;
import com.epam.rd.autocode.assessment.appliances.security.password.reset.InvalidPasswordResetException;
import com.epam.rd.autocode.assessment.appliances.security.password.reset.PasswordResetService;
import com.epam.rd.autocode.assessment.appliances.security.web.PasswordChangeLogoutHandler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.TransactionException;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Locale;

@Controller
@RequiredArgsConstructor
@Profile("local & !prod")
@ConditionalOnExpression(
        "'${store.mail.enabled:false}' == 'true' && '${store.security.auth-mode:database}' =="
                + " 'database'")
public class PasswordResetController {
    private final PasswordResetService passwords;
    private final PasswordChangeLogoutHandler logout;

    @ModelAttribute
    public void preventCaching(HttpServletResponse response) {
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        response.setHeader("Referrer-Policy", "no-referrer");
    }

    @GetMapping("/password/forgot")
    public String forgot(Model model) {
        model.addAttribute("forgotForm", new ForgotPasswordRequest());
        return "password/forgot";
    }

    @PostMapping("/password/forgot")
    public String request(
            @Valid @ModelAttribute("forgotForm") ForgotPasswordRequest form,
            BindingResult errors,
            HttpServletRequest request,
            HttpServletResponse response,
            Locale locale,
            Model model) {
        if (errors.hasErrors()) {
            response.setStatus(400);
            return "password/forgot";
        }
        try {
            long retryAfter = passwords.request(form.getEmail(), request.getRemoteAddr(), locale);
            if (retryAfter > 0) {
                response.setStatus(429);
                response.setHeader(HttpHeaders.RETRY_AFTER, Long.toString(retryAfter));
                model.addAttribute("retryAfterSeconds", retryAfter);
                return "password/forgot";
            }
        } catch (DataAccessException | TransactionException unavailable) {
            throw unavailable(unavailable);
        }
        return "redirect:/password/forgot?sent";
    }

    @GetMapping("/password/reset")
    public String reset(@RequestParam(defaultValue = "") String token, Model model) {
        var form = new PasswordResetRequest();
        // Never reflect arbitrarily large query values back into the response.
        form.setToken(token.length() <= 100 ? token : "");
        model.addAttribute("resetForm", form);
        return "password/reset";
    }

    @PostMapping("/password/reset")
    public String reset(
            @Valid @ModelAttribute("resetForm") PasswordResetRequest form,
            BindingResult errors,
            HttpServletRequest request,
            HttpServletResponse response) {
        if (errors.hasErrors()) {
            response.setStatus(400);
            return "password/reset";
        }
        try {
            passwords.reset(form);
        } catch (PasswordChangeException unchanged) {
            errors.rejectValue("newPassword", unchanged.error().messageKey());
            response.setStatus(400);
            return "password/reset";
        } catch (InvalidPasswordResetException invalid) {
            errors.reject("password.reset.invalid");
            response.setStatus(400);
            return "password/reset";
        } catch (DataAccessException | TransactionException unavailable) {
            throw unavailable(unavailable);
        }
        logout.logout(request, response);
        return "redirect:/login?passwordChanged";
    }

    private AuthenticationServiceException unavailable(RuntimeException cause) {
        return new AuthenticationServiceException(
                "Password recovery temporarily unavailable", SafeFailure.snapshot(cause));
    }
}
