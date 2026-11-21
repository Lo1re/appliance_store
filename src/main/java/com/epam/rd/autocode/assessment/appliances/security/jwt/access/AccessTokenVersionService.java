package com.epam.rd.autocode.assessment.appliances.security.jwt.access;

import com.epam.rd.autocode.assessment.appliances.model.security.AccountSecurityState;
import com.epam.rd.autocode.assessment.appliances.repository.security.AccountSecurityStateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Shared-DB credential revocation; no HTTP session or per-token denylist. */
@Component @RequiredArgsConstructor @Transactional(readOnly = true)
public class AccessTokenVersionService {
    private final AccountSecurityStateRepository states;
    public long current(Long id) { return states.findById(id).map(AccountSecurityState::getCredentialVersion).orElse(0L); }
    public void requireCurrent(Long id, long tokenVersion) {
        if (tokenVersion != current(id)) throw new AccessDeniedException("Account credentials have changed");
    }
    /** Caller holds the base user row lock used by issuance and password changes. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void revokePrevious(Long id) {
        var existing = states.findById(id);
        if (existing.isPresent()) existing.orElseThrow().advance();
        else states.save(new AccountSecurityState(id, 1));
    }
}
