package com.epam.rd.autocode.assessment.appliances.dto.order;

import java.math.BigDecimal;
import java.util.List;

public record OrderResponse(
        OrderSummary summary,
        List<Item> items,
        BigDecimal total
) {

    public record Item(
            Long id,
            Long applianceId,
            String applianceName,
            Long quantity,
            BigDecimal amount
    ) {
    }
}