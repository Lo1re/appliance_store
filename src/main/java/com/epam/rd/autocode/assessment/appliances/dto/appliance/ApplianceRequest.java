package com.epam.rd.autocode.assessment.appliances.dto.appliance;

import com.epam.rd.autocode.assessment.appliances.model.Category;
import com.epam.rd.autocode.assessment.appliances.model.PowerType;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
public class ApplianceRequest {

    @NotBlank
    @Size(max = 150)
    private String name;

    @NotNull
    private Category category;

    @NotBlank
    @Size(max = 100)
    private String model;

    @NotNull
    @Positive
    private Long manufacturerId;

    @NotNull
    private PowerType powerType;

    @NotBlank
    @Size(max = 1000)
    private String characteristic;

    @NotBlank
    @Size(max = 4000)
    private String description;

    @NotNull
    @Positive
    private Integer power;

    @NotNull
    @Positive
    @Digits(integer = 12, fraction = 2)
    private BigDecimal price;
}