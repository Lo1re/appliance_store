package com.epam.rd.autocode.assessment.appliances.service.policy;

import static org.assertj.core.api.Assertions.*;

import com.epam.rd.autocode.assessment.appliances.exception.AccountError;
import com.epam.rd.autocode.assessment.appliances.exception.AccountException;
import com.epam.rd.autocode.assessment.appliances.model.Client;
import com.epam.rd.autocode.assessment.appliances.model.Employee;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

class EmployeeDeletionPolicyTest {
    private final EmployeeDeletionPolicy policy = new EmployeeDeletionPolicy();
    private final Employee actor = new Employee(1L, "Actor", "actor@example.com", "hash", "Sales");
    private final Employee target =
            new Employee(2L, "Target", "target@example.com", "hash", "Sales");

    @Test
    void rejectsSelfDeletionWhileOtherEmployeesExist() {
        assertThatThrownBy(() -> policy.requireAllowed(actor, actor, 2, false))
                .isInstanceOfSatisfying(
                        AccountException.class,
                        error ->
                                assertThat(error.error())
                                        .isEqualTo(AccountError.EMPLOYEE_SELF_DELETE));
    }

    @Test
    void lastEmployeeRuleTakesPriorityOverSelfDeletion() {
        assertThatThrownBy(() -> policy.requireAllowed(actor, actor, 1, false))
                .isInstanceOfSatisfying(
                        AccountException.class,
                        error -> assertThat(error.error()).isEqualTo(AccountError.LAST_EMPLOYEE));
    }

    @Test
    void preservesTheOrderReferenceRestriction() {
        assertThatThrownBy(() -> policy.requireAllowed(actor, target, 2, true))
                .isInstanceOfSatisfying(
                        AccountException.class,
                        error -> assertThat(error.error()).isEqualTo(AccountError.EMPLOYEE_IN_USE));
    }

    @Test
    void allowsAnotherUnreferencedEmployeeAndRejectsNonEmployees() {
        assertThatCode(() -> policy.requireAllowed(actor, target, 2, false))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> policy.requireAllowed(new Client(), target, 2, false))
                .isInstanceOf(AccessDeniedException.class);
    }
}
