package com.epam.rd.autocode.assessment.appliances.mapper;

import com.epam.rd.autocode.assessment.appliances.dto.order.OrderItemRequest;
import com.epam.rd.autocode.assessment.appliances.dto.order.OrderRequest;
import com.epam.rd.autocode.assessment.appliances.dto.order.OrderResponse;
import com.epam.rd.autocode.assessment.appliances.dto.order.OrderSummary;
import com.epam.rd.autocode.assessment.appliances.model.OrderRow;
import com.epam.rd.autocode.assessment.appliances.model.Orders;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Mapper(config = MappingConfig.class)
public interface OrderMapper {
    @Mapping(target = "clientId", source = "client.id")
    @Mapping(target = "clientName", source = "client.name")
    @Mapping(target = "employeeId", source = "employee.id")
    @Mapping(target = "employeeName", source = "employee.name")
    @Mapping(target = "approved", source = "approved", defaultValue = "false")
    OrderSummary toSummary(Orders order);

    @Mapping(target = "summary", source = "order")
    @Mapping(target = "items", source = "order.orderRowSet", qualifiedByName = "sortedItems")
    @Mapping(target = "total", source = "total")
    OrderResponse toResponse(Orders order, BigDecimal total);

    OrderRequest toRequest(OrderResponse response);

    OrderItemRequest toRequestItem(OrderResponse.Item item);

    @Mapping(target = "applianceId", source = "appliance.id")
    @Mapping(target = "applianceName", source = "appliance.name")
    @Mapping(target = "quantity", source = "number")
    OrderResponse.Item toItem(OrderRow row);

    @Named("sortedItems")
    default List<OrderResponse.Item> sortedItems(Set<OrderRow> rows) {
        return rows.stream()
                .sorted(Comparator.comparing(row -> row.getAppliance().getId()))
                .map(this::toItem)
                .toList();
    }
}
