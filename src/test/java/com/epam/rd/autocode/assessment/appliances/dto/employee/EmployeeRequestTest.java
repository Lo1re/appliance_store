package com.epam.rd.autocode.assessment.appliances.dto.employee;

import com.epam.rd.autocode.assessment.appliances.dto.employee.EmployeeRequest;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EmployeeRequestTest {
    @Test
    void normalizesBeforeValidation() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            var employee = new EmployeeRequest();
            employee.setName(" Staff ");
            employee.setEmail("staff@example.com");
            employee.setDepartment(null);
            assertThat(validator.validate(employee)).hasSize(1);
            employee.setDepartment(" Dispatch ");
            assertThat(employee.getDepartment()).isEqualTo("Dispatch");
            assertThat(validator.validate(employee)).isEmpty();
        }
    }
}
