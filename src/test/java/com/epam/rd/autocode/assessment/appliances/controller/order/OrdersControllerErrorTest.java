package com.epam.rd.autocode.assessment.appliances.controller.order;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.epam.rd.autocode.assessment.appliances.dto.order.OrderRequest;
import com.epam.rd.autocode.assessment.appliances.exception.OrderError;
import com.epam.rd.autocode.assessment.appliances.exception.OrderException;
import com.epam.rd.autocode.assessment.appliances.service.OrderService;
import com.epam.rd.autocode.assessment.appliances.web.form.OrderFormAssembler;
import com.epam.rd.autocode.assessment.appliances.web.form.OrderFormData;
import com.epam.rd.autocode.assessment.appliances.web.pagination.ListPageFactory;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.util.List;

class OrdersControllerErrorTest {
    private final OrderService service = mock(OrderService.class);
    private final OrderFormAssembler forms = mock(OrderFormAssembler.class);
    private final OrdersController controller =
            new OrdersController(service, forms, new ListPageFactory(10));

    @ParameterizedTest
    @EnumSource(
            value = OrderError.class,
            names = {
                "CLIENT_REQUIRED",
                "CLIENT_NOT_FOUND",
                "DUPLICATE_APPLIANCE",
                "APPLIANCE_NOT_FOUND"
            })
    void invalidReferencesRedisplaySubmittedFormWithTheSpecificError(OrderError error) {
        var request = new OrderRequest();
        var errors = new BeanPropertyBindingResult(request, "orderForm");
        var response = new MockHttpServletResponse();
        var model = new ExtendedModelMap();
        when(service.create(7L, request)).thenThrow(new OrderException(error));
        when(forms.redisplay(null, 7L, request))
                .thenReturn(new OrderFormData(null, 7L, request, true, List.of(), List.of()));
        assertThat(
                        controller.save(
                                null,
                                7L,
                                request,
                                errors,
                                response,
                                model,
                                new RedirectAttributesModelMap()))
                .isEqualTo("order/form");
        assertThat(response.getStatus()).isEqualTo(400);
        assertThat(errors.getGlobalError()).isNotNull();
        assertThat(errors.getGlobalError().getCode()).isEqualTo(error.messageKey());
        assertThat(model.get("orderForm")).isSameAs(request);
        verify(service).create(7L, request);
    }

    @ParameterizedTest
    @EnumSource(
            value = OrderError.class,
            names = {
                "CLIENT_REQUIRED",
                "CLIENT_NOT_FOUND",
                "DUPLICATE_APPLIANCE",
                "APPLIANCE_NOT_FOUND"
            },
            mode = EnumSource.Mode.EXCLUDE)
    void resourceAndLifecycleFailuresKeepTheirOriginalException(OrderError error) {
        var request = new OrderRequest();
        var failure = new OrderException(error);
        when(service.update(9L, request)).thenThrow(failure);
        assertThatThrownBy(
                        () ->
                                controller.save(
                                        9L,
                                        7L,
                                        request,
                                        new BeanPropertyBindingResult(request, "orderForm"),
                                        new MockHttpServletResponse(),
                                        new ExtendedModelMap(),
                                        new RedirectAttributesModelMap()))
                .isSameAs(failure);
        verifyNoInteractions(forms);
    }
}
