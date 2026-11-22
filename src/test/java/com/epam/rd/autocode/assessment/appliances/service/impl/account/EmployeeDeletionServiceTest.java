package com.epam.rd.autocode.assessment.appliances.service.impl.account;

import com.epam.rd.autocode.assessment.appliances.logging.BusinessAudit;
import com.epam.rd.autocode.assessment.appliances.mapper.EmployeeMapper;
import com.epam.rd.autocode.assessment.appliances.model.Employee;
import com.epam.rd.autocode.assessment.appliances.repository.EmployeeRepository;
import com.epam.rd.autocode.assessment.appliances.repository.OrdersRepository;
import com.epam.rd.autocode.assessment.appliances.security.identity.CurrentActor;
import com.epam.rd.autocode.assessment.appliances.service.policy.EmployeeDeletionPolicy;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;

class EmployeeDeletionServiceTest {
    @Test
    void locksBeforeResolvingTheActorCountingAndDeleting() {
        var repository = mock(EmployeeRepository.class);
        var orders = mock(OrdersRepository.class);
        var currentActor = mock(CurrentActor.class);
        var actor = new Employee(1L, "Actor", "actor@example.com", "hash", "Sales");
        var target = new Employee(2L, "Target", "target@example.com", "hash", "Sales");
        when(currentActor.requireUser()).thenReturn(actor);
        when(repository.findById(2L)).thenReturn(Optional.of(target));
        when(repository.count()).thenReturn(2L);
        var credentials = mock(AccountCredentialsService.class);
        var mutationLocks = mock(AccountMutationLocks.class);
        var service = new EmployeeServiceImpl(repository, orders, mock(EmployeeMapper.class),
                credentials, mutationLocks, currentActor, new EmployeeDeletionPolicy(), mock(BusinessAudit.class));
        service.delete(2L);
        var sequence = inOrder(mutationLocks, repository, currentActor, orders);
        sequence.verify(mutationLocks).lockEmployeeAccounts();
        sequence.verify(mutationLocks).lockAccount(2L, com.epam.rd.autocode.assessment.appliances.exception.AccountError.EMPLOYEE_NOT_FOUND);
        sequence.verify(repository).findAllForDeletionLock();
        sequence.verify(currentActor).requireUser();
        sequence.verify(repository).findById(2L);
        sequence.verify(repository).count();
        sequence.verify(orders).existsByEmployee_Id(2L);
        sequence.verify(repository).delete(target);
    }
}
