package com.epam.rd.autocode.assessment.appliances.service.impl;

import com.epam.rd.autocode.assessment.appliances.exception.AccountException;
import com.epam.rd.autocode.assessment.appliances.model.Employee;
import com.epam.rd.autocode.assessment.appliances.repository.EmployeeRepository;
import com.epam.rd.autocode.assessment.appliances.repository.OrdersRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceImplTest {
    @Mock private EmployeeRepository repository;
    @Mock private OrdersRepository dependencies;
    @InjectMocks private EmployeeServiceImpl service;

    @Test
    void refusesDeletionWhileReferencedAndDoesNotCallDelete() {
        var entity = new Employee();
        entity.setId(42L);
        when(repository.findById(42L)).thenReturn(Optional.of(entity));
        when(dependencies.existsByEmployee_Id(42L)).thenReturn(true);

        assertThatThrownBy(() -> service.delete(42L))
                .isInstanceOf(AccountException.class)
                .hasMessage("employee.in-use");
        verify(repository, never()).delete(entity);
    }
}
