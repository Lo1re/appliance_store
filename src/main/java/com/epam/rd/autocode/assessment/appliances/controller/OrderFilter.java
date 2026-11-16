package com.epam.rd.autocode.assessment.appliances.controller;

import java.util.Arrays;

enum OrderFilter {
    ALL("all", null), PENDING("pending", false), APPROVED("approved", true);

    private final String parameter;
    private final Boolean approved;

    OrderFilter(String parameter, Boolean approved) {
        this.parameter = parameter;
        this.approved = approved;
    }

    static OrderFilter fromParameter(String parameter) {
        return Arrays.stream(values())
                .filter(value -> value.parameter.equals(parameter))
                .findFirst().orElse(ALL);
    }

    String parameter() {
        return parameter;
    }

    Boolean approved() {
        return approved;
    }
}
