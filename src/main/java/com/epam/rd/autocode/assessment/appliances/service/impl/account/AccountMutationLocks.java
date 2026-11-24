package com.epam.rd.autocode.assessment.appliances.service.impl.account;

import com.epam.rd.autocode.assessment.appliances.exception.AccountError;
import com.epam.rd.autocode.assessment.appliances.exception.AccountException;
import com.epam.rd.autocode.assessment.appliances.repository.lock.AccountRowLock;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;

// Acquires account mutation locks inside the caller's transaction.
@Component
@RequiredArgsConstructor
public class AccountMutationLocks {
    private final AccountRowLock accounts;

    void lockEmployeeAccounts() {
        accounts.lockEmployeeAccounts();
    }

    void lockAccount(Long id, AccountError missing) {
        accounts.lock(id).orElseThrow(() -> new AccountException(missing));
    }
}
