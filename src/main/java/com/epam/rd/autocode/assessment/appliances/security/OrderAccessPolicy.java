package com.epam.rd.autocode.assessment.appliances.security;

import com.epam.rd.autocode.assessment.appliances.exception.OrderException;
import com.epam.rd.autocode.assessment.appliances.model.Client;
import com.epam.rd.autocode.assessment.appliances.model.Employee;
import com.epam.rd.autocode.assessment.appliances.model.Orders;
import com.epam.rd.autocode.assessment.appliances.model.User;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

@Component
public class OrderAccessPolicy {

    public void requireAccess(User actor, Orders order) {
        if (actor instanceof Employee) {
            return;
        }

        if (!(actor instanceof Client client)) {
            throw new AccessDeniedException("Access denied");
        }

        if (!client.getId().equals(order.getClient().getId())) {
            throw new OrderException("order.not-found");
        }
    }

    public Employee requireEmployee(User actor) {
        if (actor instanceof Employee employee) {
            return employee;
        }

        throw new AccessDeniedException("Access denied");
    }

    public void requireClientSelection(Client actor, Long requestedClientId) {
        if (requestedClientId != null
                && !actor.getId().equals(requestedClientId)) {
            throw new AccessDeniedException("Access denied");
        }
    }
}
