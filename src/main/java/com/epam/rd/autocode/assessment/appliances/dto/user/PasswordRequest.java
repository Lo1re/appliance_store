package com.epam.rd.autocode.assessment.appliances.dto.user;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.nio.charset.StandardCharsets;

@Getter
@Setter
public class PasswordRequest {

    @NotBlank
    @Size(min = 12)
    @Pattern(
            //at least one Uppercase, lowercase, digit, symbol,
            regexp = "(?s)^(?=.*\\p{Lu})(?=.*\\p{Ll})(?=.*\\p{Nd})(?=.*[\\p{P}\\p{S}]).*$",
            message = "{user.password.complexity}"
    )
    private String password;

    @AssertTrue(message = "{user.password.too-long}")
    public boolean isWithinBcryptLimit() {
        //bcs 2 condtion needs not null
        return password == null || password.getBytes(StandardCharsets.UTF_8).length <= 72;
    }
}