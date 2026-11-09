package com.epam.rd.autocode.assessment.appliances.dto.client;

import com.epam.rd.autocode.assessment.appliances.dto.user.UserProfileRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class ClientRequest extends UserProfileRequest {

    @NotBlank
    @Size(max = 32)
    private String card;

    public void setCard(String card) {
        this.card = card == null ? null : card.strip();
    }
}