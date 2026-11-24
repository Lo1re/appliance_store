package com.epam.rd.autocode.assessment.appliances.security.authorization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.epam.rd.autocode.assessment.appliances.exception.OrderException;
import com.epam.rd.autocode.assessment.appliances.model.Client;
import com.epam.rd.autocode.assessment.appliances.model.Employee;
import com.epam.rd.autocode.assessment.appliances.model.Orders;
import com.epam.rd.autocode.assessment.appliances.model.User;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

class OrderAccessPolicyTest {

    private final OrderAccessPolicy policy = new OrderAccessPolicy();

    @Test
    void employeeCanAccessAnyOrderAndApprove() {
        var employee = new Employee();
        employee.setId(10L);

        Orders order = orderFor(client(1L));

        assertThatCode(() -> policy.requireAccess(employee, order)).doesNotThrowAnyException();
        assertThat(policy.requireEmployee(employee)).isSameAs(employee);
    }

    @Test
    void clientCanAccessOwnOrder() {
        Client owner = client(1L);
        Orders order = orderFor(owner);

        assertThatCode(() -> policy.requireAccess(owner, order)).doesNotThrowAnyException();
    }

    @Test
    void anotherClientCannotAccessOrder() {
        Orders order = orderFor(client(1L));

        assertThatThrownBy(() -> policy.requireAccess(client(2L), order))
                .isInstanceOf(OrderException.class)
                .hasMessage("order.not-found");
    }

    @Test
    void unsupportedActorCannotAccessOrder() {
        Orders order = orderFor(client(1L));

        assertThatThrownBy(() -> policy.requireAccess(new User(), order))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void clientCannotApproveOrder() {
        assertThatThrownBy(() -> policy.requireEmployee(client(1L)))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void clientCanSelectOnlyOwnAccount() {
        Client owner = client(1L);

        assertThatCode(() -> policy.requireClientSelection(owner, null)).doesNotThrowAnyException();
        assertThatCode(() -> policy.requireClientSelection(owner, 1L)).doesNotThrowAnyException();
        assertThatThrownBy(() -> policy.requireClientSelection(owner, 2L))
                .isInstanceOf(AccessDeniedException.class);
    }

    private Client client(Long id) {
        var client = new Client();
        client.setId(id);
        return client;
    }

    private Orders orderFor(Client client) {
        var order = new Orders();
        order.setClient(client);
        return order;
    }
}
