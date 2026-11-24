package com.epam.rd.autocode.assessment.appliances.dto.query;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Size;

import lombok.Getter;
import lombok.Setter;

// HTTP parameters shared by list pages; resource-specific filters stay separate.
@Getter
@Setter
public class ListQuery {
    @Size(max = 100)
    private String search = "";

    @Max(1_000_000)
    private int page;

    @Size(max = 100)
    private String sort;
}
