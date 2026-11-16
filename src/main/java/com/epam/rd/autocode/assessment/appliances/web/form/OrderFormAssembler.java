package com.epam.rd.autocode.assessment.appliances.web.form;

import com.epam.rd.autocode.assessment.appliances.dto.client.ClientResponse;
import com.epam.rd.autocode.assessment.appliances.dto.order.OrderItemRequest;
import com.epam.rd.autocode.assessment.appliances.dto.order.OrderRequest;
import com.epam.rd.autocode.assessment.appliances.mapper.OrderMapper;
import com.epam.rd.autocode.assessment.appliances.security.AccountRole;
import com.epam.rd.autocode.assessment.appliances.security.CurrentActor;
import com.epam.rd.autocode.assessment.appliances.service.ApplianceService;
import com.epam.rd.autocode.assessment.appliances.service.ClientService;
import com.epam.rd.autocode.assessment.appliances.service.OrderService;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('CLIENT', 'EMPLOYEE')")
public class OrderFormAssembler {
    private final OrderService orderService;
    private final OrderMapper orderMapper;
    private final ApplianceService applianceService;
    private final ClientService clientService;
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
        Pageable options = Pageable.unpaged(Sort.by("name", "id"));
        var products = applianceService.findAll("", options).getContent();
        var clients = selectClient ? clientService.findAll("", options).getContent()
                : List.<ClientResponse>of();
        return new OrderFormData(id, clientId, request, selectClient, products, clients);
    }
}
