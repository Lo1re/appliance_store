package com.epam.rd.autocode.assessment.appliances.dto.order;

public record OrderSummary(
        Long id,
        Long clientId,
        String clientName,
        Long employeeId,
        String employeeName,
        boolean approved
) {
}