package com.epam.rd.autocode.assessment.appliances.dto.employee;

import com.epam.rd.autocode.assessment.appliances.dto.user.UserProfileRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class EmployeeRequest extends UserProfileRequest {

    @NotBlank
    @Size(max = 100)
    private String department;

    public void setDepartment(String department) {
        this.department = department == null ? null : department.strip();
    }
}
