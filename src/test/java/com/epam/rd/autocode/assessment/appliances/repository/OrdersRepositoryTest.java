package com.epam.rd.autocode.assessment.appliances.repository;

import com.epam.rd.autocode.assessment.appliances.model.Client;
import com.epam.rd.autocode.assessment.appliances.model.OrderRow;
import com.epam.rd.autocode.assessment.appliances.model.Orders;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class OrdersRepositoryTest {
    @Autowired EntityManager entityManager;
    @Autowired UserRepository users;
    @Autowired ApplianceRepository appliances;
    @Autowired OrdersRepository orders;

    @Test
    void detailedOrderFetchesRowsWhereasPagedOrderFetchesOnlyItsSummary() {
        var client = (Client) users.findByEmailIgnoreCase("mercury@gmail.com").orElseThrow();
        var appliance = appliances.search("Claw", PageRequest.of(0, 1)).getContent().get(0);
        var order = new Orders();
        order.setClient(client);
        order.getOrderRowSet().add(new OrderRow(null, appliance, 1L, appliance.getPrice()));
        Long id = orders.saveAndFlush(order).getId();
        entityManager.clear();

        var util = entityManager.getEntityManagerFactory().getPersistenceUnitUtil();
        var summary = orders.findByClient_IdAndApproved(client.getId(), false, PageRequest.of(0, 5))
                .getContent().get(0);
        assertThat(util.isLoaded(summary, "client")).isTrue();
        assertThat(util.isLoaded(summary, "orderRowSet")).isFalse();
        entityManager.clear();

        var details = orders.findDetailedById(id).orElseThrow();
        assertThat(util.isLoaded(details, "orderRowSet")).isTrue();
        assertThat(details.getOrderRowSet()).singleElement().satisfies(row -> {
            assertThat(row.getAppliance().getName()).isEqualTo("Claw");
            assertThat(util.isLoaded(row.getAppliance(), "manufacturer")).isTrue();
        });
    }
}
