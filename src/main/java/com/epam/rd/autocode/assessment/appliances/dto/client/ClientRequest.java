package com.epam.rd.autocode.assessment.appliances.dto.client;

import com.epam.rd.autocode.assessment.appliances.dto.user.UserProfileRequest;
import jakarta.validation.constraints.Size;
import com.epam.rd.autocode.assessment.appliances.validation.LoyaltyCardNumber;
import lombok.Getter;

@Getter
public class ClientRequest extends UserProfileRequest {

    @Size(max = 32)
    private String card;

    public void setCard(String card) {
        this.card = LoyaltyCardNumber.normalize(card);
    }
}