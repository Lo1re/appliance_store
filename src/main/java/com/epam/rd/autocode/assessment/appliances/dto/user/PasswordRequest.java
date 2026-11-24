package com.epam.rd.autocode.assessment.appliances.dto.user;

import com.epam.rd.autocode.assessment.appliances.validation.password.StrongPassword;

import jakarta.validation.constraints.NotBlank;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PasswordRequest {

    @NotBlank @StrongPassword private String password;
}
