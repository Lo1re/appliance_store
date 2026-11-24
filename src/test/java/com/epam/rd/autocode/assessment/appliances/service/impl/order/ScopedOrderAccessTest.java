package com.epam.rd.autocode.assessment.appliances.service.impl.order;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.epam.rd.autocode.assessment.appliances.exception.OrderException;
import com.epam.rd.autocode.assessment.appliances.logging.BusinessAudit;
import com.epam.rd.autocode.assessment.appliances.mapper.OrderMapper;
import com.epam.rd.autocode.assessment.appliances.model.Client;
import com.epam.rd.autocode.assessment.appliances.repository.ClientRepository;
import com.epam.rd.autocode.assessment.appliances.repository.OrdersRepository;
import com.epam.rd.autocode.assessment.appliances.security.authorization.OrderAccessPolicy;
import com.epam.rd.autocode.assessment.appliances.security.identity.CurrentActor;

import org.junit.jupiter.api.Test;

class ScopedOrderAccessTest {
    private final OrdersRepository orders = mock(OrdersRepository.class);
    private final CurrentActor actor = mock(CurrentActor.class);
    private final OrderMapper mapper = mock(OrderMapper.class);
    private final OrderServiceImpl service =
            new OrderServiceImpl(
                    orders,
                    actor,
                    new OrderAccessPolicy(),
                    mock(ClientRepository.class),
                    mock(OrderRowsFactory.class),
                    mapper,
                    mock(BusinessAudit.class));

    @Test
    void clientReadNeverFallsBackToLoadingSomeoneElsesOrder() {
        var client = new Client();
        client.setId(3L);
        when(actor.requireUser()).thenReturn(client);
        assertThatThrownBy(() -> service.findById(99L))
                .isInstanceOf(OrderException.class)
                .hasMessage("order.not-found");
        verify(orders).findDetailedByIdAndClient_Id(99L, 3L);
        verify(orders, never()).findDetailedById(anyLong());
        verifyNoInteractions(mapper);
    }

    @Test
    void clientDeleteNeverLocksAnOrderOutsideItsOwnershipScope() {
        var client = new Client();
        client.setId(3L);
        when(actor.requireUser()).thenReturn(client);
        assertThatThrownBy(() -> service.delete(99L))
                .isInstanceOf(OrderException.class)
                .hasMessage("order.not-found");
        verify(orders).findOwnedForUpdate(99L, 3L);
        verify(orders, never()).findForUpdate(anyLong());
        verify(orders, never()).delete(any());
    }
}
