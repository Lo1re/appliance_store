package com.epam.rd.autocode.assessment.appliances.dto.auth;

import com.epam.rd.autocode.assessment.appliances.validation.normalization.EmailAddress;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ApiLoginRequest {
    @NotBlank
    @Email
    @Size(max = 254)
    private String email;

    @NotBlank
    @Size(max = 1024)
    private String password;

    public void setEmail(String email) {
        this.email = EmailAddress.normalize(email);
    }
}
