package com.epam.rd.autocode.assessment.appliances.dto.employee;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EmployeeResponse {

    private Long id;
    private String name;
    private String email;
    private String department;
}
