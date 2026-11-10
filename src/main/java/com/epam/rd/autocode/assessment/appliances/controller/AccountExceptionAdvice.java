package com.epam.rd.autocode.assessment.appliances.controller;

import com.epam.rd.autocode.assessment.appliances.exception.AccountException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Slf4j
@ControllerAdvice(assignableTypes = {ClientController.class, EmployeeController.class})
public class AccountExceptionAdvice {

    @ExceptionHandler(AccountException.class)
    public String handleBusinessError(
            AccountException exception, HandlerMethod handler, RedirectAttributes redirect
    ) {
        redirect.addFlashAttribute("errorKey", exception.error().messageKey());

        return ClientController.class.isAssignableFrom(handler.getBeanType())
                ? "redirect:/clients"
                : "redirect:/employees";
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ModelAndView handleDatabaseError(
            DataIntegrityViolationException exception, HandlerMethod handler
    ) {
        log.error("Database integrity error in {}.{} ({})",
                handler.getBeanType().getSimpleName(),
                handler.getMethod().getName(),
                exception.getClass().getSimpleName());

        var view = new ModelAndView("error/account");
        view.setStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        return view;
    }
}