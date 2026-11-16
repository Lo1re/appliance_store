package com.epam.rd.autocode.assessment.appliances.service.impl;

import com.epam.rd.autocode.assessment.appliances.exception.AccountException;
import com.epam.rd.autocode.assessment.appliances.model.Employee;
import com.epam.rd.autocode.assessment.appliances.repository.EmployeeRepository;
import com.epam.rd.autocode.assessment.appliances.repository.OrdersRepository;
import com.epam.rd.autocode.assessment.appliances.security.CurrentActor;
import com.epam.rd.autocode.assessment.appliances.service.policy.EmployeeDeletionPolicy;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import com.epam.rd.autocode.assessment.appliances.logging.BusinessAudit;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceImplTest {
    @Mock private BusinessAudit audit;
    @Mock private EmployeeRepository repository;
    @Mock private OrdersRepository dependencies;
    @Mock private CurrentActor currentActor;
    @Spy private EmployeeDeletionPolicy deletionPolicy = new EmployeeDeletionPolicy();
    @InjectMocks private EmployeeServiceImpl service;

    @Test
    void refusesDeletionWhileReferencedAndDoesNotCallDelete() {
        var entity = new Employee();
        entity.setId(42L);
        var actor = new Employee();
        actor.setId(7L);
        when(currentActor.requireUser()).thenReturn(actor);
        when(repository.count()).thenReturn(2L);
        when(repository.findById(42L)).thenReturn(Optional.of(entity));
        when(dependencies.existsByEmployee_Id(42L)).thenReturn(true);

        assertThatThrownBy(() -> service.delete(42L))
                .isInstanceOf(AccountException.class)
                .hasMessage("employee.in-use");
        verify(repository, never()).delete(entity);
    }
}
