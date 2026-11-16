package com.epam.rd.autocode.assessment.appliances.dto.appliance;

import com.epam.rd.autocode.assessment.appliances.model.Category;
import com.epam.rd.autocode.assessment.appliances.model.PowerType;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ApplianceResponse {

    private Long id;
    private String name;
    private Category category;
    private String model;
    private Long manufacturerId;
    private String manufacturerName;
    private PowerType powerType;
    private String characteristic;
    private String description;
    private Integer power;
    private BigDecimal price;
}
