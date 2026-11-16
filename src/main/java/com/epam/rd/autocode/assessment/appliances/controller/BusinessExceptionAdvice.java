package com.epam.rd.autocode.assessment.appliances.controller;

import com.epam.rd.autocode.assessment.appliances.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Slf4j
@ControllerAdvice(annotations = ControllerSection.class)
public class BusinessExceptionAdvice {
    @ExceptionHandler(BusinessException.class)
    public String handleBusinessError(BusinessException exception, HandlerMethod handler,
                                      RedirectAttributes redirect) {
        redirect.addFlashAttribute("errorKey", exception.error().messageKey());
        return "redirect:" + section(handler).path();
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ModelAndView handleDatabaseError(DataIntegrityViolationException exception,
                                            HandlerMethod handler) {
        log.error("Database integrity error in {}.{}", handler.getBeanType().getSimpleName(),
                handler.getMethod().getName(), exception);
        var view = new ModelAndView(section(handler).errorView());
        view.setStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        return view;
    }

    private ControllerSection section(HandlerMethod handler) {
        return java.util.Objects.requireNonNull(AnnotatedElementUtils.findMergedAnnotation(
                handler.getBeanType(), ControllerSection.class));
    }
}
