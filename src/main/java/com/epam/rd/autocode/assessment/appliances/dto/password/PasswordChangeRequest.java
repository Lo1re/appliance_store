package com.epam.rd.autocode.assessment.appliances.dto.password;

import com.epam.rd.autocode.assessment.appliances.validation.password.PasswordConfirmation;
import com.epam.rd.autocode.assessment.appliances.validation.password.PasswordsMatch;
import com.epam.rd.autocode.assessment.appliances.validation.password.StrongPassword;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@PasswordsMatch
public class PasswordChangeRequest implements PasswordConfirmation {
    @NotBlank
    @Size(max = 1024)
    private String currentPassword;

    @NotBlank @StrongPassword private String newPassword;

    @NotBlank
    @Size(max = 1024)
    private String confirmPassword;

    @Override
    public String toString() {
        return "PasswordChangeRequest[credentials=REDACTED]";
    }
}
