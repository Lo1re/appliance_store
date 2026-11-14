package com.epam.rd.autocode.assessment.appliances.integration;

import com.epam.rd.autocode.assessment.appliances.exception.AccountException;
import com.epam.rd.autocode.assessment.appliances.exception.CatalogException;
import com.epam.rd.autocode.assessment.appliances.model.*;
import com.epam.rd.autocode.assessment.appliances.repository.*;
import com.epam.rd.autocode.assessment.appliances.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:catalog-accounts-it",
})
class OrderReferencesIntegrationTest {

    private static final PageRequest PAGE = PageRequest.of(0, 30, Sort.by("name"));

    @Autowired ApplianceService appliances;
    @Autowired ClientService clients;
    @Autowired EmployeeService employees;
    @Autowired UserRepository users;
    @Autowired OrdersRepository orders;
    @Autowired ApplianceInOrderRepository rows;
    @Autowired ClientRepository clientRepository;
    @Autowired EmployeeRepository employeeRepository;
    @Autowired ApplianceRepository applianceRepository;

    @Test
    void protectsEntitiesReferencedByAnOrder() {
        Client client = (Client) users.findByEmailIgnoreCase("venus@gmail.com").orElseThrow();
        Employee employee = (Employee) users.findByEmailIgnoreCase("phobos@gmail.com").orElseThrow();
        Appliance appliance = applianceRepository.search("Claw", PAGE).getContent().get(0);

        Orders order = new Orders();
        order.setClient(client);
        order.setEmployee(employee);
        order.getOrderRowSet().add(new OrderRow(null, appliance, 1L, appliance.getPrice()));
        Long orderId = orders.saveAndFlush(order).getId();

        try {
            assertThat(rows.existsByAppliance_Id(appliance.getId())).isTrue();
            assertThat(clientRepository.existsById(client.getId())).isTrue();
            assertThat(employeeRepository.existsById(employee.getId())).isTrue();

            assertThatThrownBy(() -> appliances.delete(appliance.getId()))
                    .isInstanceOf(CatalogException.class).hasMessage("appliance.in-use");
            assertThatThrownBy(() -> clients.delete(client.getId()))
                    .isInstanceOf(AccountException.class).hasMessage("client.in-use");
            assertThatThrownBy(() -> employees.delete(employee.getId()))
                    .isInstanceOf(AccountException.class).hasMessage("employee.in-use");
        } finally {
            orders.deleteById(orderId);
        }
    }
}