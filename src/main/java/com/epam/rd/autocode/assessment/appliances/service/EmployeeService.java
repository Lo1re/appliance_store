package com.epam.rd.autocode.assessment.appliances.service;

import com.epam.rd.autocode.assessment.appliances.dto.employee.EmployeeRequest;
import com.epam.rd.autocode.assessment.appliances.dto.employee.EmployeeResponse;
import com.epam.rd.autocode.assessment.appliances.dto.user.PasswordRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface EmployeeService {

    Page<EmployeeResponse> findAll(String search, Pageable pageable);

    EmployeeResponse findById(Long id);

    EmployeeResponse create(
            @NotNull @Valid EmployeeRequest request,
            @NotNull @Valid PasswordRequest password
    );

    EmployeeResponse update(Long id, @NotNull @Valid EmployeeRequest request);

    void delete(Long id);
}