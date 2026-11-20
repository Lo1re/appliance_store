package com.epam.rd.autocode.assessment.appliances.service;

import com.epam.rd.autocode.assessment.appliances.dto.order.*;
import com.epam.rd.autocode.assessment.appliances.exception.OrderError;
import com.epam.rd.autocode.assessment.appliances.exception.OrderException;
import com.epam.rd.autocode.assessment.appliances.mapper.OrderMapper;
import com.epam.rd.autocode.assessment.appliances.security.AccountRole;
import com.epam.rd.autocode.assessment.appliances.security.CurrentActor;
import com.epam.rd.autocode.assessment.appliances.web.form.OrderFormAssembler;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.data.domain.Page;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class OrderFormAssemblerTest {
    private final CurrentActor actor = mock(CurrentActor.class);

    private final OrderService orders = mock(OrderService.class);
    private final LookupService lookups = mock(LookupService.class);
    private final OrderFormAssembler forms = new OrderFormAssembler(orders, Mappers.getMapper(OrderMapper.class), lookups, actor);

    @Test
    void loadsClientOptionsOnlyForEmployeeCreation() {
        when(lookups.products(eq(""), any())).thenReturn(List.of());
        var clientForm = forms.create();
        assertThat(clientForm.selectClient()).isFalse();
        assertThat(clientForm.request().getItems()).hasSize(1);
        verify(lookups, never()).clients(anyString(), any());
        when(lookups.clients(eq(""), any())).thenReturn(List.of());
        when(actor.hasRole(AccountRole.EMPLOYEE)).thenReturn(true);
        assertThat(forms.create().selectClient()).isTrue();
        verify(lookups).clients(eq(""), any());
    }

    @Test
    void authorizesEditBeforeLoadingOptionsAndNeverOffersAnotherClient() {
        when(orders.findForEditing(7L)).thenReturn(new OrderResponse(
                new OrderSummary(7L, 1L, "Buyer", null, null, false),
                List.of(new OrderResponse.Item(2L, 3L, "Product", 4L, new BigDecimal("20.00"))),
                new BigDecimal("20.00")));
        when(lookups.products(eq(""), any())).thenReturn(List.of());
        var form = forms.edit(7L);
        assertThat(form.editing()).isTrue();
        assertThat(form.selectClient()).isFalse();
        assertThat(form.request().getItems()).singleElement().satisfies(item -> {
            assertThat(item.getApplianceId()).isEqualTo(3L);
            assertThat(item.getQuantity()).isEqualTo(4L);
        });
        var sequence = inOrder(orders, lookups);
        sequence.verify(orders).findForEditing(7L);
        sequence.verify(lookups).products(eq(""), any());
        verify(lookups, never()).clients(anyString(), any());
    }

    @Test
    void invalidInputCannotRedisplayAnOrderThatIsNoLongerEditable() {
        doThrow(new OrderException(OrderError.NOT_EDITABLE)).when(orders).requireEditable(7L);
        assertThatThrownBy(() -> forms.redisplay(7L, null, new OrderRequest()))
                .isInstanceOf(OrderException.class).hasMessage("order.not-editable");
        verifyNoInteractions(lookups);
    }

    @Test
    void redisplayKeepsSubmittedValuesAndSelectedClient() {
        when(lookups.products(eq(""), any())).thenReturn(List.of());
        when(lookups.clients(eq(""), any())).thenReturn(List.of());
        var item = new OrderItemRequest();
        item.setApplianceId(5L);
        item.setQuantity(0L);
        var request = new OrderRequest();
        request.setItems(List.of(item));
        when(actor.hasRole(AccountRole.EMPLOYEE)).thenReturn(true);
        var form = forms.redisplay(null, 12L, request);
        assertThat(form.request()).isSameAs(request);
        assertThat(form.request().getItems()).containsExactly(item);
        assertThat(form.selectedClientId()).isEqualTo(12L);
        verify(lookups).products("", List.of(5L));
        verify(lookups).clients("", List.of(12L));
        verifyNoInteractions(orders);
    }
}
