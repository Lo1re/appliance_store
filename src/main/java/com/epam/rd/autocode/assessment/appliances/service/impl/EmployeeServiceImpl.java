package com.epam.rd.autocode.assessment.appliances.service.impl;

import com.epam.rd.autocode.assessment.appliances.dto.employee.EmployeeRequest;
import com.epam.rd.autocode.assessment.appliances.dto.employee.EmployeeResponse;
import com.epam.rd.autocode.assessment.appliances.dto.user.PasswordRequest;
import com.epam.rd.autocode.assessment.appliances.exception.AccountException;
import com.epam.rd.autocode.assessment.appliances.mapper.EmployeeMapper;
import com.epam.rd.autocode.assessment.appliances.model.Employee;
import com.epam.rd.autocode.assessment.appliances.repository.EmployeeRepository;
import com.epam.rd.autocode.assessment.appliances.repository.OrdersRepository;
import com.epam.rd.autocode.assessment.appliances.service.EmployeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EmployeeServiceImpl implements EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final OrdersRepository ordersRepository;
    private final EmployeeMapper employeeMapper;
    private final AccountSupport accountSupport;

    @Override
    public Page<EmployeeResponse> findAll(String search, Pageable pageable) {
        if (search == null || search.isBlank()) {
            return employeeRepository.findAll(pageable)
                    .map(employeeMapper::toResponse);
        }

        String query = search.strip();
        return employeeRepository
                .findByNameContainingIgnoreCaseOrEmailContainingIgnoreCase(query, query, pageable)
                .map(employeeMapper::toResponse);
    }

    @Override
    public EmployeeResponse findById(Long id) {
        return employeeMapper.toResponse(requireEmployee(id));
    }

    @Override
    @Transactional
    public EmployeeResponse create(EmployeeRequest request, PasswordRequest password) {
        accountSupport.requireAvailableEmail(request.getEmail(), null);

        Employee employee = new Employee();
        employeeMapper.applyRequest(request, employee);
        accountSupport.setPassword(employee, password.getPassword());

        Employee saved = employeeRepository.save(employee);
        return employeeMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public EmployeeResponse update(Long id, EmployeeRequest request) {
        Employee employee = requireEmployee(id);
        accountSupport.requireAvailableEmail(request.getEmail(), id);

        employeeMapper.applyRequest(request, employee);
        return employeeMapper.toResponse(employee);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Employee employee = requireEmployee(id);

        if (ordersRepository.existsByEmployee_Id(id)) {
            throw new AccountException("employee.in-use");
        }

        employeeRepository.delete(employee);
    }

    private Employee requireEmployee(Long id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new AccountException("employee.not-found"));
    }
}