package com.epam.rd.autocode.assessment.appliances.controller.registration;

import com.epam.rd.autocode.assessment.appliances.dto.registration.ClientRegistrationRequest;
import com.epam.rd.autocode.assessment.appliances.exception.AccountException;
import com.epam.rd.autocode.assessment.appliances.logging.SafeFailure;
import com.epam.rd.autocode.assessment.appliances.security.registration.ConditionalOnClientRegistration;
import com.epam.rd.autocode.assessment.appliances.security.registration.InvalidRegistrationException;
import com.epam.rd.autocode.assessment.appliances.security.registration.RegistrationConfirmation;
import com.epam.rd.autocode.assessment.appliances.security.registration.RegistrationSubmission;
import com.epam.rd.autocode.assessment.appliances.web.form.AccountFormView;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.dao.DataAccessException;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.TransactionException;
import org.springframework.ui.Model;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Locale;

@Controller
@RequiredArgsConstructor
@ConditionalOnClientRegistration
public class ClientRegistrationController {
    private final RegistrationSubmission submissions;
    private final RegistrationConfirmation confirmations;
    private final AccountFormView forms;

    @ModelAttribute
    public void preventCaching(HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("Referrer-Policy", "no-referrer");
    }

    @GetMapping("/register")
    public String form(Model model) {
        model.addAttribute("registrationForm", new ClientRegistrationRequest());
        return "registration/form";
    }

    @PostMapping("/register")
    public String request(
            @Valid @ModelAttribute("registrationForm") ClientRegistrationRequest form,
            BindingResult errors,
            HttpServletRequest request,
            HttpServletResponse response,
            Model model,
            Locale locale,
            RedirectAttributes redirects) {
        if (errors.hasErrors()) {
            response.setStatus(400);
            return "registration/form";
        }
        try {
            long retry = submissions.submit(form, request.getRemoteAddr(), locale);
            if (retry > 0) {
                response.setStatus(429);
                response.setHeader("Retry-After", Long.toString(retry));
                model.addAttribute("retryAfterSeconds", retry);
                return "registration/form";
            }
            return "redirect:/register/check-email";
        } catch (AccountException conflict) {
            forms.rejectDuplicate(conflict, errors);
            response.setStatus(409);
            return "registration/form";
        } catch (DataAccessException | TransactionException unavailable) {
            throw unavailable(unavailable);
        }
    }

    @GetMapping("/register/check-email")
    public String checkEmail() {
        return "registration/check-email";
    }

    @GetMapping({"/register/verify", "/register/confirm"})
    public String verify(
            @RequestParam(defaultValue = "") String token,
            Model model,
            RedirectAttributes redirects) {
        try {
            confirmations.preview(token);
            model.addAttribute("registrationToken", token);
            return "registration/confirm";
        } catch (InvalidRegistrationException invalid) {
            return "redirect:/register?invalid";
        } catch (DataAccessException | TransactionException unavailable) {
            throw unavailable(unavailable);
        }
    }

    @PostMapping("/register/complete")
    public String complete(
            @RequestParam(defaultValue = "") String token,
            HttpServletResponse response,
            Model model,
            RedirectAttributes redirects) {
        try {
            // Only the token is accepted: role, email, ID and password cannot replace the stored
            // application.
            confirmations.complete(token);
            return "redirect:/login?registered";
        } catch (AccountException conflict) {
            try {
                var details = confirmations.preview(token);
                var form = new ClientRegistrationRequest();
                form.setName(details.name());
                form.setEmail(details.email());
                form.setCard(details.card());
                var errors = new BeanPropertyBindingResult(form, "registrationForm");
                forms.rejectDuplicate(conflict, errors);
                model.addAttribute("registrationForm", form);
                model.addAttribute(BindingResult.MODEL_KEY_PREFIX + "registrationForm", errors);
                response.setStatus(409);
                return "registration/form";
            } catch (InvalidRegistrationException invalid) {
                return "redirect:/register?invalid";
            } catch (DataAccessException | TransactionException unavailable) {
                throw unavailable(unavailable);
            }
        } catch (InvalidRegistrationException invalid) {
            return "redirect:/register?invalid";
        } catch (DataAccessException | TransactionException unavailable) {
            throw unavailable(unavailable);
        }
    }

    private AuthenticationServiceException unavailable(RuntimeException failure) {
        return new AuthenticationServiceException(
                "Registration temporarily unavailable", SafeFailure.snapshot(failure));
    }
}
