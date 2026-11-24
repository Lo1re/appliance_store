package com.epam.rd.autocode.assessment.appliances.service.impl.account;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.epam.rd.autocode.assessment.appliances.exception.AccountError;
import com.epam.rd.autocode.assessment.appliances.exception.AccountException;
import com.epam.rd.autocode.assessment.appliances.model.Client;
import com.epam.rd.autocode.assessment.appliances.repository.lock.AccountRowLock;

import org.junit.jupiter.api.Test;

import java.util.Optional;

class AccountMutationLocksTest {
    @Test
    void propagatesMissingAccountAndPreservesUnderlyingLockOperations() {
        var rows = mock(AccountRowLock.class);
        var locks = new AccountMutationLocks(rows);
        assertThatThrownBy(() -> locks.lockAccount(4L, AccountError.CLIENT_NOT_FOUND))
                .isInstanceOf(AccountException.class)
                .hasMessage("client.not-found");
        when(rows.lock(4L)).thenReturn(Optional.of(new Client()));
        locks.lockAccount(4L, AccountError.CLIENT_NOT_FOUND);
        locks.lockEmployeeAccounts();
        verify(rows).lockEmployeeAccounts();
    }
}
