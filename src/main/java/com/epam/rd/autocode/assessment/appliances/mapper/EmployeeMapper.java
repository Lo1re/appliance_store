package com.epam.rd.autocode.assessment.appliances.mapper;

import com.epam.rd.autocode.assessment.appliances.dto.employee.EmployeeRequest;
import com.epam.rd.autocode.assessment.appliances.dto.employee.EmployeeResponse;
import com.epam.rd.autocode.assessment.appliances.model.Employee;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EmployeeMapper {

    private final ModelMapper modelMapper;

    public void applyRequest(EmployeeRequest request, Employee employee) {
        employee.setName(request.getName());
        employee.setEmail(request.getEmail());
        employee.setDepartment(request.getDepartment());
    }

    public EmployeeResponse toResponse(Employee employee) {
        return modelMapper.map(employee, EmployeeResponse.class);
    }

    public EmployeeRequest toRequest(EmployeeResponse response) {
        var request = new EmployeeRequest();
        request.setName(response.getName());
        request.setEmail(response.getEmail());
        request.setDepartment(response.getDepartment());
        return request;
    }
}