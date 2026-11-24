package com.epam.rd.autocode.assessment.appliances.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RefreshTokenRequest(@NotBlank @Size(max = 256) String refreshToken) {
    @Override
    public String toString() {
        return "RefreshTokenRequest[credential=REDACTED]";
    }
}
