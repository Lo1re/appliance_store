package com.epam.rd.autocode.assessment.appliances.logging;

import java.util.Objects;

public record BusinessChange(BusinessAction action, Long actorId, Long entityId, String requestId) {
    public BusinessChange {
        Objects.requireNonNull(action);
        Objects.requireNonNull(entityId);
        requestId = Objects.requireNonNullElse(requestId, "-");
    }
}
