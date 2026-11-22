package com.epam.rd.autocode.assessment.appliances.dto.oauth;

import com.epam.rd.autocode.assessment.appliances.validation.EmailAddress;
import com.epam.rd.autocode.assessment.appliances.validation.LoyaltyCardNumber;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class GoogleProfileRequest {
    @NotBlank @Size(max = 100) private String name;
    @NotBlank @Email @Size(max = 254) private String email;
    @Size(max = 32) private String card;
    public void setName(String value) { name = value == null ? null : value.strip(); }
    public void setEmail(String value) { email = EmailAddress.normalize(value); }
    public void setCard(String value) { card = LoyaltyCardNumber.normalize(value); }
    @Override public String toString() { return "GoogleProfileRequest[redacted]"; }
}
