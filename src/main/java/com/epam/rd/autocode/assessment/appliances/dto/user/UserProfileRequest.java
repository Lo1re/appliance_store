package com.epam.rd.autocode.assessment.appliances.dto.user;

import com.epam.rd.autocode.assessment.appliances.validation.normalization.EmailAddress;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.Getter;

@Getter
public class UserProfileRequest {

    @NotBlank
    @Size(max = 100)
    private String name;

    @NotBlank
    @Email
    @Size(max = 254)
    private String email;

    public void setName(String name) {
        this.name = name == null ? null : name.strip();
    }

    public void setEmail(String email) {
        this.email = EmailAddress.normalize(email);
    }
}
