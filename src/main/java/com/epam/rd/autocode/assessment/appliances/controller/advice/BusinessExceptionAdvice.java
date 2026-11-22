package com.epam.rd.autocode.assessment.appliances.controller.advice;

import com.epam.rd.autocode.assessment.appliances.exception.BusinessException;
import com.epam.rd.autocode.assessment.appliances.logging.SafeFailure;
import com.epam.rd.autocode.assessment.appliances.web.error.BusinessErrorStatus;
import com.epam.rd.autocode.assessment.appliances.web.error.MvcErrorViews;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.ModelAndView;

@Slf4j
@Order(0)
@ControllerAdvice(annotations = ControllerSection.class)
public class BusinessExceptionAdvice {
    @ExceptionHandler(BusinessException.class)
    public ModelAndView handleBusinessError(BusinessException exception, HandlerMethod handler) {
        HttpStatus status = BusinessErrorStatus.of(exception);
        return MvcErrorViews.render(status)
                .addObject("errorMessageKey", exception.error().messageKey())
                .addObject("returnPath", section(handler).path());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ModelAndView handleDatabaseError(DataIntegrityViolationException exception,
                                            HandlerMethod handler) {
        log.error("Database integrity error in {}.{}", handler.getBeanType().getSimpleName(),
                handler.getMethod().getName(), SafeFailure.snapshot(exception));
        var view = new ModelAndView(section(handler).errorView());
        view.setStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        view.addObject("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        view.addObject("returnPath", section(handler).path());
        return view;
    }

    private ControllerSection section(HandlerMethod handler) {
        return java.util.Objects.requireNonNull(AnnotatedElementUtils.findMergedAnnotation(
                handler.getBeanType(), ControllerSection.class));
    }
}
