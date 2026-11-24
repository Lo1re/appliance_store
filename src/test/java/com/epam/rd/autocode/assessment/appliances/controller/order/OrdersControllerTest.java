package com.epam.rd.autocode.assessment.appliances.controller.order;

import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.epam.rd.autocode.assessment.appliances.controller.advice.BusinessExceptionAdvice;
import com.epam.rd.autocode.assessment.appliances.dto.order.OrderResponse;
import com.epam.rd.autocode.assessment.appliances.dto.order.OrderSummary;
import com.epam.rd.autocode.assessment.appliances.exception.OrderError;
import com.epam.rd.autocode.assessment.appliances.exception.OrderException;
import com.epam.rd.autocode.assessment.appliances.mapper.OrderMapper;
import com.epam.rd.autocode.assessment.appliances.security.identity.CurrentActor;
import com.epam.rd.autocode.assessment.appliances.service.OrderService;
import com.epam.rd.autocode.assessment.appliances.service.lookup.LookupService;
import com.epam.rd.autocode.assessment.appliances.web.form.OrderFormAssembler;
import com.epam.rd.autocode.assessment.appliances.web.pagination.ListPageFactory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;

@ExtendWith(MockitoExtension.class)
class OrdersControllerTest {

    @Mock private OrderService orders;
    @Mock private LookupService lookups;

    private final CurrentActor actor = mock(CurrentActor.class);

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc =
                MockMvcBuilders.standaloneSetup(
                                new OrdersController(
                                        orders,
                                        new OrderFormAssembler(
                                                orders,
                                                Mappers.getMapper(OrderMapper.class),
                                                lookups,
                                                actor),
                                        new ListPageFactory(10)))
                        .setControllerAdvice(new BusinessExceptionAdvice())
                        .build();
    }

    @Test
    void fallsBackToUnfilteredFirstPageForInvalidParameters() throws Exception {
        var firstPage = PageRequest.of(0, 10, Sort.by("id"));
        when(orders.findAll(null, firstPage)).thenReturn(Page.empty(firstPage));

        mvc.perform(
                        get("/orders")
                                .param("page", "-1")
                                .param("status", "unknown")
                                .param("sort", "name"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("status", "all"))
                .andExpect(model().attribute("sort", "id"));

        verify(orders).findAll(null, firstPage);
    }

    @Test
    void approvedOrderCannotOpenAnEditForm() throws Exception {
        when(orders.findForEditing(7L)).thenThrow(new OrderException(OrderError.NOT_EDITABLE));
        mvc.perform(get("/orders/7/edit"))
                .andExpect(status().isConflict())
                .andExpect(view().name("error/409"))
                .andExpect(model().attribute("errorMessageKey", "order.not-editable"));
        verifyNoInteractions(lookups);
    }

    @Test
    void allowsAnExistingOrderWithoutRowsToBeRepaired() throws Exception {
        when(orders.findForEditing(7L))
                .thenReturn(
                        new OrderResponse(
                                new OrderSummary(7L, 1L, "Buyer", null, null, false),
                                List.of(),
                                BigDecimal.ZERO));
        when(lookups.products(eq(""), any())).thenReturn(java.util.List.of());

        mvc.perform(get("/orders/7/edit"))
                .andExpect(status().isOk())
                .andExpect(view().name("order/form"))
                .andExpect(model().attribute("orderForm", hasProperty("items", hasSize(1))))
                .andExpect(model().attribute("selectClient", false));
        verify(lookups, never()).clients(anyString(), any());
    }
}
