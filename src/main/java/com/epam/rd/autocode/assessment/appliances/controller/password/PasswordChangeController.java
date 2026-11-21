package com.epam.rd.autocode.assessment.appliances.controller.password;

import com.epam.rd.autocode.assessment.appliances.dto.password.PasswordChangeRequest;
import com.epam.rd.autocode.assessment.appliances.exception.password.PasswordChangeError;
import com.epam.rd.autocode.assessment.appliances.exception.password.PasswordChangeException;
import com.epam.rd.autocode.assessment.appliances.logging.SafeFailure;
import com.epam.rd.autocode.assessment.appliances.security.web.PasswordChangeLogoutHandler;
import com.epam.rd.autocode.assessment.appliances.service.password.PasswordChangeService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataAccessException;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.LockedException;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.TransactionException;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
@ConditionalOnProperty(name = "store.security.auth-mode", havingValue = "database", matchIfMissing = true)
public class PasswordChangeController {
    private static final String PAGE = "password/change";
    private final PasswordChangeService passwords;
    private final PasswordChangeLogoutHandler logout;
    @GetMapping("/password/change")
    public String form(Model model, HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
        model.addAttribute("passwordForm", new PasswordChangeRequest()); return PAGE;
    }
    @PostMapping("/password/change")
    public String change(@Valid @ModelAttribute("passwordForm") PasswordChangeRequest form, BindingResult errors,
                         HttpServletRequest request, HttpServletResponse response, RedirectAttributes redirects) {
        response.setHeader("Cache-Control", "no-store");
        if (errors.hasErrors()) { response.setStatus(400); return PAGE; }
        try {
            passwords.change(form);
        } catch (PasswordChangeException rejected) {
            String field = rejected.error() == PasswordChangeError.CURRENT_INVALID ? "currentPassword" : "newPassword";
            errors.rejectValue(field, rejected.error().messageKey()); response.setStatus(400); return PAGE;
        } catch (LockedException locked) {
            errors.reject("login.locked"); response.setStatus(429); return PAGE;
        } catch (DataAccessException | TransactionException unavailable) {
            throw new AuthenticationServiceException("Password change temporarily unavailable", SafeFailure.snapshot(unavailable));
        }
        logout.logout(request, response);
        return "redirect:/login?passwordChanged";
    }
}
