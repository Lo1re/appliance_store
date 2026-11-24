package com.epam.rd.autocode.assessment.appliances.dto.password;

import com.epam.rd.autocode.assessment.appliances.validation.normalization.EmailAddress;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.Getter;

@Getter
public class ForgotPasswordRequest {
    @NotBlank
    @Email
    @Size(max = 254)
    private String email;

    public void setEmail(String email) {
        this.email = EmailAddress.normalize(email);
    }

    @Override
    public String toString() {
        return "ForgotPasswordRequest[redacted]";
    }
}
