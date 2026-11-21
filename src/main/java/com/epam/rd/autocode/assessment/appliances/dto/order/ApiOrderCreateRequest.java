package com.epam.rd.autocode.assessment.appliances.dto.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ApiOrderCreateRequest(@Positive Long clientId, @NotNull @Valid OrderRequest order) { }
