package com.epam.rd.autocode.assessment.appliances.web.form;

import com.epam.rd.autocode.assessment.appliances.dto.lookup.SelectOption;
import com.epam.rd.autocode.assessment.appliances.dto.order.OrderRequest;

import java.util.List;

public record OrderFormData(
        Long orderId,
        Long selectedClientId,
        OrderRequest request,
        boolean selectClient,
        List<SelectOption> products,
        List<SelectOption> clients) {
    public OrderFormData {
        products = List.copyOf(products);
        clients = List.copyOf(clients);
    }

    public boolean editing() {
        return orderId != null;
    }
}
