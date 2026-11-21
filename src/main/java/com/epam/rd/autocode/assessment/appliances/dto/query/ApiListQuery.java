package com.epam.rd.autocode.assessment.appliances.dto.query;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ApiListQuery {
    @Size(max = 100) private String search = "";
    @Min(0) @Max(1_000_000) private int page;
    @Min(1) @Max(100) private Integer size;
    @Size(max = 100) private String sort;
}
