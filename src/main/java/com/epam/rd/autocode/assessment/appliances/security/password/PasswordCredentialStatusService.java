package com.epam.rd.autocode.assessment.appliances.security.password;

import com.epam.rd.autocode.assessment.appliances.model.security.PasswordCredentialState;
import com.epam.rd.autocode.assessment.appliances.repository.oauth.OAuthIdentityRepository;
import com.epam.rd.autocode.assessment.appliances.repository.password.PasswordCredentialStateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Explicit local-credential lifecycle; an OAuth association alone cannot prove password absence. */
@Component @RequiredArgsConstructor @Transactional(readOnly = true)
public class PasswordCredentialStatusService {
    private final PasswordCredentialStateRepository states;
    private final OAuthIdentityRepository identities;
    public PasswordCredentialStatus status(Long userId) {
        return states.findById(userId).map(state -> state.isPasswordSet() ? PasswordCredentialStatus.SET : PasswordCredentialStatus.NOT_SET)
                .orElseGet(() -> identities.existsByProviderAndUserId("GOOGLE", userId) ? PasswordCredentialStatus.UNKNOWN : PasswordCredentialStatus.SET);
    }
    public boolean explicitlyMissing(Long userId) { return states.findById(userId).map(state -> !state.isPasswordSet()).orElse(false); }
    @Transactional(propagation = Propagation.MANDATORY)
    public void initializeGoogle(Long userId) { states.save(new PasswordCredentialState(userId, false)); }
    @Transactional(propagation = Propagation.MANDATORY)
    public void rememberExistingPassword(Long userId) {
        if (!states.existsById(userId)) states.save(new PasswordCredentialState(userId, true));
    }
    @Transactional(propagation = Propagation.MANDATORY)
    public void markSet(Long userId) {
        var existing = states.findById(userId);
        if (existing.isPresent()) existing.orElseThrow().markSet();
        else if (identities.existsByProviderAndUserId("GOOGLE", userId)) states.save(new PasswordCredentialState(userId, true));
    }
}
