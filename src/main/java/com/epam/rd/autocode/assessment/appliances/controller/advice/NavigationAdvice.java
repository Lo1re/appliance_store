package com.epam.rd.autocode.assessment.appliances.controller.advice;

import com.epam.rd.autocode.assessment.appliances.security.authentication.AuthenticationProperties;
import com.epam.rd.autocode.assessment.appliances.security.password.reset.PasswordResetService;
import com.epam.rd.autocode.assessment.appliances.web.LanguageNavigation;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
@RequiredArgsConstructor
public class NavigationAdvice {
    private final LanguageNavigation languages;
    private final AuthenticationProperties authentication;
    private final ObjectProvider<PasswordResetService> passwordReset;

    @ModelAttribute("passwordResetEnabled")
    public boolean passwordResetEnabled() { return passwordReset.getIfAvailable() != null; }

    @ModelAttribute("passwordChangeEnabled")
    public boolean passwordChangeEnabled() { return authentication.authMode() == AuthenticationProperties.Mode.DATABASE; }



    @ModelAttribute("languageLinks")
    public LanguageNavigation.Links languageLinks(HttpServletRequest request) {
        return languages.links(request);
    }

    @ModelAttribute("navigation")
    public Navigation navigation(HttpServletRequest request) {
        return new Navigation(
                request.getUserPrincipal() != null,
                request.isUserInRole("EMPLOYEE")
        );
    }

    public record Navigation(boolean signedIn, boolean employee) {
    }
}