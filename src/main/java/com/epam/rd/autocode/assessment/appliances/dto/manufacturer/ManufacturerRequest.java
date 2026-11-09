package com.epam.rd.autocode.assessment.appliances.dto.manufacturer;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ManufacturerRequest {

    @NotBlank(message = "{manufacturer.name.required}")
    @Size(max = 150, message = "{manufacturer.name.size}")
    private String name;

    public void setName(String name) {
        this.name = name == null ? null : name.strip();
    }
}