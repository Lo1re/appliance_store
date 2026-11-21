package com.epam.rd.autocode.assessment.appliances.mapper;

import com.epam.rd.autocode.assessment.appliances.model.Appliance;
import com.epam.rd.autocode.assessment.appliances.model.Client;
import com.epam.rd.autocode.assessment.appliances.model.Employee;
import com.epam.rd.autocode.assessment.appliances.model.OrderRow;
import com.epam.rd.autocode.assessment.appliances.model.Orders;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import static org.assertj.core.api.Assertions.assertThat;

class OrderMapperTest {
    @Test
    void orderDetailsExposeStableItemOrderAndKeepTheServerTotal() {
        var order = new Orders();
        order.setId(100L);
        order.setClient(new Client(7L, "Buyer", "buyer@example.com", "hash", "DEMO"));
        order.setEmployee(new Employee(8L, "Staff", "staff@example.com", "hash", "Sales"));
        order.setApproved(true);

        var later = new Appliance();
        later.setId(20L);
        later.setName("Second product");
        var earlier = new Appliance();
        earlier.setId(10L);
        earlier.setName("First product");
        order.getOrderRowSet().add(new OrderRow(1L, later, 1L, new BigDecimal("7.50")));
        order.getOrderRowSet().add(new OrderRow(2L, earlier, 2L, new BigDecimal("10.00")));

        var response = Mappers.getMapper(OrderMapper.class).toResponse(order, new BigDecimal("17.50"));
        assertThat(response.summary().id()).isEqualTo(100L);
        assertThat(response.summary().clientName()).isEqualTo("Buyer");
        assertThat(response.summary().employeeName()).isEqualTo("Staff");
        assertThat(response.items()).extracting(item -> item.applianceId()).containsExactly(10L, 20L);
        assertThat(response.items().get(0).applianceName()).isEqualTo("First product");
        assertThat(response.items().get(0).quantity()).isEqualTo(2L);
        assertThat(response.items().get(0).amount()).isEqualByComparingTo("10.00");
        assertThat(response.total()).isEqualByComparingTo("17.50");
    }
    @Test
    void pendingOrderMapsWithoutAnEmployeeAndCanBeConvertedToAnEditableRequest() {
        var order = new Orders();
        order.setId(1L);
        order.setClient(new Client(2L, "Buyer", "buyer@example.com", "hash", "CARD"));
        var appliance = new Appliance();
        appliance.setId(3L);
        appliance.setName("Product");
        order.getOrderRowSet().add(new OrderRow(4L, appliance, 2L, new BigDecimal("9.00")));
        var mapper = Mappers.getMapper(OrderMapper.class);
        var response = mapper.toResponse(order, new BigDecimal("9.00"));
        assertThat(response.summary().employeeId()).isNull();
        assertThat(response.summary().employeeName()).isNull();
        assertThat(response.summary().approved()).isFalse();
        assertThat(mapper.toRequest(response).getItems()).singleElement().satisfies(item -> {
            assertThat(item.getApplianceId()).isEqualTo(3L);
            assertThat(item.getQuantity()).isEqualTo(2L);
        });
    }
}
