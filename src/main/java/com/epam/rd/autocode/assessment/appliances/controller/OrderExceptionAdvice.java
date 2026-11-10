package com.epam.rd.autocode.assessment.appliances.controller;

import com.epam.rd.autocode.assessment.appliances.exception.OrderException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@ControllerAdvice(assignableTypes = OrdersController.class)
public class OrderExceptionAdvice {

    @ExceptionHandler(OrderException.class)
    public String handleBusinessError(
            OrderException exception,
            RedirectAttributes redirect
    ) {
        redirect.addFlashAttribute("errorKey", exception.getMessage());
        return "redirect:/orders";
    }
}