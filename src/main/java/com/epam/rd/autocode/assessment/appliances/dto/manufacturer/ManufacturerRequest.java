package com.epam.rd.autocode.assessment.appliances.dto.manufacturer;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ManufacturerRequest {

    @NotBlank(message = "{manufacturer.name.required}")
    @Size(max = 150, message = "{manufacturer.name.size}")
    private String name;
}