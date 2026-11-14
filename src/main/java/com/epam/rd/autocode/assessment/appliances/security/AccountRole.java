package com.epam.rd.autocode.assessment.appliances.security;

import com.epam.rd.autocode.assessment.appliances.model.Client;
import com.epam.rd.autocode.assessment.appliances.model.Employee;
import com.epam.rd.autocode.assessment.appliances.model.User;

import java.util.Optional;

public enum AccountRole {

    CLIENT,
    EMPLOYEE;

    public String authority() {
        return "ROLE_" + name();
    }

    public static Optional<AccountRole> from(User user) {
        if (user instanceof Client) {
            return Optional.of(CLIENT);
        }

        if (user instanceof Employee) {
            return Optional.of(EMPLOYEE);
        }

        return Optional.empty();
    }
}