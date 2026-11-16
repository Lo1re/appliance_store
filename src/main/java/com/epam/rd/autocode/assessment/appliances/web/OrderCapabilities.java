package com.epam.rd.autocode.assessment.appliances.web;

import com.epam.rd.autocode.assessment.appliances.dto.order.OrderResponse;

public record OrderCapabilities(boolean canEdit, boolean canDelete, boolean canApprove) {
    public static OrderCapabilities forOrder(OrderResponse order, boolean employee) {
        boolean editable = !order.summary().approved();
        return new OrderCapabilities(editable, editable, employee && editable);
    }
}
