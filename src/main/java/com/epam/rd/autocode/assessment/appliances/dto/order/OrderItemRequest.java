package com.epam.rd.autocode.assessment.appliances.dto.order;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderItemRequest {

    @NotNull
    @Positive
    private Long applianceId;

    @NotNull
    @Positive
    @Max(10_000)
    private Long quantity;
}
