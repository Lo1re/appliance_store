package com.epam.rd.autocode.assessment.appliances.mapper;

import com.epam.rd.autocode.assessment.appliances.dto.employee.EmployeeRequest;
import com.epam.rd.autocode.assessment.appliances.mapper.EmployeeMapper;
import com.epam.rd.autocode.assessment.appliances.model.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import static org.assertj.core.api.Assertions.assertThat;

class EmployeeMapperTest {
    private final ObjectMapper json = new ObjectMapper();

    @Test
    void keepsIdentityAndPasswordAndOmitsPasswordFromResponse() {
        var employee = new Employee(8L, "Old name", "old@example.com", "stored-hash", "OLD");
        var employeeRequest = new EmployeeRequest();
        employeeRequest.setName(" Staff ");
        employeeRequest.setEmail(" STAFF@EXAMPLE.COM ");
        employeeRequest.setDepartment(" Sales ");
        var employeeMapper = Mappers.getMapper(EmployeeMapper.class);
        employeeMapper.applyRequest(employeeRequest, employee);
        var employeeResponse = employeeMapper.toResponse(employee);
        assertThat(employeeResponse.getId()).isEqualTo(8L);
        assertThat(employeeResponse.getName()).isEqualTo("Staff");
        assertThat(employeeResponse.getEmail()).isEqualTo("old@example.com");
        assertThat(employeeResponse.getDepartment()).isEqualTo("Sales");
        assertThat(employee.getPassword()).isEqualTo("stored-hash");
        assertThat(json.valueToTree(employeeResponse).has("password")).isFalse();
    }
}
