package com.epam.rd.autocode.assessment.appliances.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class NavigationAdvice {

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