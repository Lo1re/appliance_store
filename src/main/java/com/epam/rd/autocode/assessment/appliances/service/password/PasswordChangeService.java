package com.epam.rd.autocode.assessment.appliances.service.password;

import com.epam.rd.autocode.assessment.appliances.dto.password.PasswordChangeRequest;
import com.epam.rd.autocode.assessment.appliances.exception.password.PasswordChangeError;
import com.epam.rd.autocode.assessment.appliances.exception.password.PasswordChangeException;
import com.epam.rd.autocode.assessment.appliances.repository.LoginAttemptRepository;
import com.epam.rd.autocode.assessment.appliances.repository.RefreshTokenRepository;
import com.epam.rd.autocode.assessment.appliances.repository.UserRepository;
import com.epam.rd.autocode.assessment.appliances.repository.lock.AccountRowLock;
import com.epam.rd.autocode.assessment.appliances.repository.password.PasswordResetRepository;
import com.epam.rd.autocode.assessment.appliances.security.identity.CurrentActor;
import com.epam.rd.autocode.assessment.appliances.security.password.CurrentPasswordVerifier;
import com.epam.rd.autocode.assessment.appliances.security.password.PasswordChanged;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.Clock;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@RequiredArgsConstructor
@ConditionalOnProperty(name = "store.security.auth-mode", havingValue = "database", matchIfMissing = true)
@PreAuthorize("hasAnyRole('CLIENT', 'EMPLOYEE')")
public class PasswordChangeService {
    private final CurrentActor actor;
    private final AccountRowLock accounts;
    private final UserRepository users;
    private final CurrentPasswordVerifier currentPassword;
    private final PasswordEncoder encoder;
    private final RefreshTokenRepository refresh;
    private final LoginAttemptRepository attempts;
    private final ApplicationEventPublisher events;
    private final Clock clock;
    private final PasswordResetRepository resetTokens;

    @Transactional
    public void change(@NotNull @Valid PasswordChangeRequest request) {
        Long id = actor.requireUser().getId();
        accounts.lock(id).orElseThrow(() -> new AccessDeniedException("Account no longer available"));
        var credentials = users.findCredentialSnapshot(id).orElseThrow(() -> new AccessDeniedException("Account no longer available"));
        currentPassword.verify(id, credentials.email(), request.getCurrentPassword(), credentials.passwordHash());
        if (encoder.matches(request.getNewPassword(), credentials.passwordHash()))
            throw new PasswordChangeException(PasswordChangeError.NEW_UNCHANGED);
        if (users.replacePassword(id, credentials.passwordHash(), encoder.encode(request.getNewPassword())) != 1)
            throw new PasswordChangeException(PasswordChangeError.CURRENT_INVALID);
        refresh.revokeAll(id, clock.instant());
        resetTokens.deleteAll(id);
        attempts.findLocked(credentials.email()).ifPresent(attempts::delete);
        events.publishEvent(new PasswordChanged(id));
    }
}
