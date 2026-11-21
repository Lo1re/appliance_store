package com.epam.rd.autocode.assessment.appliances.repository;

import com.epam.rd.autocode.assessment.appliances.model.Client;
import com.epam.rd.autocode.assessment.appliances.model.OrderRow;
import com.epam.rd.autocode.assessment.appliances.model.Orders;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import static org.assertj.core.api.Assertions.assertThat;

@org.springframework.context.annotation.Import(com.epam.rd.autocode.assessment.appliances.config.demo.DemoMigrationConfiguration.class)
@DataJpaTest
class OrderRowRepositoryTest {
    @Autowired private OrderRowRepository rows;
    @Autowired private ApplianceRepository appliances;
    @Autowired private OrdersRepository orders;
    @Autowired private UserRepository users;

    @Test
    void tracksWhetherAnApplianceAppearsInAnOrder() {
        var appliance = appliances.search("Claw", PageRequest.of(0, 1)).getContent().get(0);
        var client = (Client) users.findByEmailIgnoreCase("mercury@gmail.com").orElseThrow();
        assertThat(rows.existsByAppliance_Id(appliance.getId())).isFalse();

        var order = new Orders();
        order.setClient(client);
        order.getOrderRowSet().add(new OrderRow(null, appliance, 1L, appliance.getPrice()));
        orders.saveAndFlush(order);
        assertThat(rows.existsByAppliance_Id(appliance.getId())).isTrue();

        orders.delete(order);
        orders.flush();
        assertThat(rows.existsByAppliance_Id(appliance.getId())).isFalse();
    }
}
