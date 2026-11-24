package com.epam.rd.autocode.assessment.appliances.service.policy;

import com.epam.rd.autocode.assessment.appliances.exception.AccountError;
import com.epam.rd.autocode.assessment.appliances.exception.AccountException;
import com.epam.rd.autocode.assessment.appliances.model.Employee;
import com.epam.rd.autocode.assessment.appliances.model.User;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

@Component
public class EmployeeDeletionPolicy {
    public void requireAllowed(User actor, Employee target, long employeeCount, boolean hasOrders) {
        if (!(actor instanceof Employee)) {
            throw new AccessDeniedException("Access denied");
        }
        if (employeeCount <= 1) {
            throw new AccountException(AccountError.LAST_EMPLOYEE);
        }
        if (actor.getId().equals(target.getId())) {
            throw new AccountException(AccountError.EMPLOYEE_SELF_DELETE);
        }
        if (hasOrders) {
            throw new AccountException(AccountError.EMPLOYEE_IN_USE);
        }
    }
}
