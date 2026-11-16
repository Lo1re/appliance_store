package com.epam.rd.autocode.assessment.appliances.web.form;

import com.epam.rd.autocode.assessment.appliances.dto.appliance.ApplianceResponse;
import com.epam.rd.autocode.assessment.appliances.dto.client.ClientResponse;
import com.epam.rd.autocode.assessment.appliances.dto.order.OrderRequest;
import java.util.List;

public record OrderFormData(
        Long orderId,
        Long selectedClientId,
        OrderRequest request,
        boolean selectClient,
        List<ApplianceResponse> products,
        List<ClientResponse> clients
) {
    public OrderFormData {
        products = List.copyOf(products);
        clients = List.copyOf(clients);
    }

    public boolean editing() {
        return orderId != null;
    }
}
