package com.epam.rd.autocode.assessment.appliances.web.form;

import com.epam.rd.autocode.assessment.appliances.dto.lookup.SelectOption;
import com.epam.rd.autocode.assessment.appliances.dto.order.OrderItemRequest;
import com.epam.rd.autocode.assessment.appliances.dto.order.OrderRequest;
import com.epam.rd.autocode.assessment.appliances.mapper.OrderMapper;
import com.epam.rd.autocode.assessment.appliances.security.identity.AccountRole;
import com.epam.rd.autocode.assessment.appliances.security.identity.CurrentActor;
import com.epam.rd.autocode.assessment.appliances.service.OrderService;
import com.epam.rd.autocode.assessment.appliances.service.lookup.LookupService;

import lombok.RequiredArgsConstructor;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Component
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('CLIENT', 'EMPLOYEE')")
public class OrderFormAssembler {
    private final OrderService orderService;
    private final OrderMapper orderMapper;
    private final LookupService lookups;
    private final CurrentActor currentActor;

    public OrderFormData create() {
        return prepare(null, null, new OrderRequest());
    }

    public OrderFormData edit(Long id) {
        OrderRequest request = orderMapper.toRequest(orderService.findForEditing(id));
        return prepare(id, null, request);
    }

    public OrderFormData redisplay(Long id, Long clientId, OrderRequest request) {
        if (id != null) {
            orderService.requireEditable(id);
        }
        return prepare(id, clientId, request);
    }

    private OrderFormData prepare(Long id, Long clientId, OrderRequest request) {
        if (request.getItems() == null || request.getItems().isEmpty()) {
            request.setItems(new ArrayList<>(List.of(new OrderItemRequest())));
        }
        boolean selectClient = id == null && currentActor.hasRole(AccountRole.EMPLOYEE);
        var selected =
                request.getItems().stream()
                        .filter(Objects::nonNull)
                        .map(OrderItemRequest::getApplianceId)
                        .filter(Objects::nonNull)
                        .distinct()
                        .limit(50)
                        .toList();
        var products = lookups.products("", selected);
        var clients =
                selectClient
                        ? lookups.clients("", clientId == null ? List.of() : List.of(clientId))
                        : List.<SelectOption>of();
        return new OrderFormData(id, clientId, request, selectClient, products, clients);
    }
}
