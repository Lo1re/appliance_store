package com.epam.rd.autocode.assessment.appliances.mapper;

import com.epam.rd.autocode.assessment.appliances.dto.order.OrderItemRequest;
import com.epam.rd.autocode.assessment.appliances.dto.order.OrderRequest;
import com.epam.rd.autocode.assessment.appliances.dto.order.OrderResponse;
import com.epam.rd.autocode.assessment.appliances.dto.order.OrderSummary;
import com.epam.rd.autocode.assessment.appliances.model.Employee;
import com.epam.rd.autocode.assessment.appliances.model.OrderRow;
import com.epam.rd.autocode.assessment.appliances.model.Orders;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Component
public class OrderMapper {

    public OrderSummary toSummary(Orders order) {
        Employee employee = order.getEmployee();

        return new OrderSummary(
                order.getId(),
                order.getClient().getId(),
                order.getClient().getName(),
                employee == null ? null : employee.getId(),
                employee == null ? null : employee.getName(),
                Boolean.TRUE.equals(order.getApproved())
        );
    }

    public OrderResponse toResponse(Orders order, BigDecimal total) {
        List<OrderResponse.Item> items = order.getOrderRowSet().stream()
                .sorted(Comparator.comparing(row -> row.getAppliance().getId()))
                .map(this::toItem)
                .toList();

        return new OrderResponse(toSummary(order), items, total);
    }

    public OrderRequest toRequest(OrderResponse response) {
        List<OrderItemRequest> items = new ArrayList<>();

        for (OrderResponse.Item item : response.items()) {
            var requestItem = new OrderItemRequest();
            requestItem.setApplianceId(item.applianceId());
            requestItem.setQuantity(item.quantity());
            items.add(requestItem);
        }

        var request = new OrderRequest();
        request.setItems(items);
        return request;
    }

    private OrderResponse.Item toItem(OrderRow row) {
        return new OrderResponse.Item(
                row.getId(),
                row.getAppliance().getId(),
                row.getAppliance().getName(),
                row.getNumber(),
                row.getAmount()
        );
    }
}