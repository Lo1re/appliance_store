package com.epam.rd.autocode.assessment.appliances.controller;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/** HTTP parameters shared by list pages; resource-specific filters stay separate. */
@Getter
@Setter
public class ListQuery {
    @Size(max = 100)
    private String search = "";
    private int page;
    @Size(max = 100)
    private String sort;
}
