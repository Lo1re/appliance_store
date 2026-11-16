package com.epam.rd.autocode.assessment.appliances.controller;

import com.epam.rd.autocode.assessment.appliances.exception.*;
import org.junit.jupiter.api.Test;
import org.springframework.web.method.HandlerMethod;
import static org.assertj.core.api.Assertions.assertThat;

class BusinessExceptionAdviceTest {
    private final BusinessExceptionAdvice advice = new BusinessExceptionAdvice();

    @Test
    void mapsTypedErrorKindsToHttpStatusesWithoutChangingTheirMessageKeys() throws Exception {
        var handler = new HandlerMethod(new Endpoint(), Endpoint.class.getMethod("action"));
        check(handler, OrderError.NOT_FOUND, 404);
        check(handler, OrderError.NOT_EDITABLE, 409);
        check(handler, OrderError.ALREADY_APPROVED, 409);
        check(handler, OrderError.CLIENT_REQUIRED, 400);
        var conflict = advice.handleBusinessError(new AccountException(AccountError.EMAIL_DUPLICATE), handler);
        assertThat(conflict.getStatus().value()).isEqualTo(409);
        assertThat(conflict.getModel().get("errorMessageKey")).isEqualTo("user.email.duplicate");
    }

    private void check(HandlerMethod handler, OrderError error, int code) {
        var view = advice.handleBusinessError(new OrderException(error), handler);
        assertThat(view.getStatus().value()).isEqualTo(code);
        assertThat(view.getModel().get("status")).isEqualTo(code);
        assertThat(view.getModel().get("errorMessageKey")).isEqualTo(error.messageKey());
        assertThat(view.getModel().get("returnPath")).isEqualTo("/orders");
    }

    @ControllerSection(path = "/orders", errorView = "error/general")
    public static class Endpoint {
        public void action() {
        }
    }
}
