package com.epam.rd.autocode.assessment.appliances.security.password.reset;

import com.epam.rd.autocode.assessment.appliances.dto.password.PasswordResetRequest;
import com.epam.rd.autocode.assessment.appliances.exception.password.PasswordChangeError;
import com.epam.rd.autocode.assessment.appliances.exception.password.PasswordChangeException;
import com.epam.rd.autocode.assessment.appliances.mail.PasswordResetMail;
import com.epam.rd.autocode.assessment.appliances.mail.SecurityMailLinkFactory;
import com.epam.rd.autocode.assessment.appliances.repository.UserRepository;
import com.epam.rd.autocode.assessment.appliances.repository.lock.AccountRowLock;
import com.epam.rd.autocode.assessment.appliances.repository.password.PasswordResetRepository;
import com.epam.rd.autocode.assessment.appliances.security.password.PasswordReplacementEffects;
import com.epam.rd.autocode.assessment.appliances.validation.normalization.EmailAddress;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import lombok.RequiredArgsConstructor;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.Clock;
import java.util.Locale;

// Anonymous credential-recovery boundary; intentionally not an authenticated application @Service.
@Component
@Validated
@RequiredArgsConstructor
@Profile("local & !prod")
@ConditionalOnExpression(
        "'${store.mail.enabled:false}' == 'true' && '${store.security.auth-mode:database}' =="
                + " 'database'")
public class PasswordResetService {
    private final UserRepository users;
    private final AccountRowLock accounts;
    private final PasswordResetRepository tokens;
    private final ResetTokenCodec codec;
    private final ResetRequestLimiter limits;
    private final PasswordEncoder encoder;
    private final ApplicationEventPublisher events;
    private final Clock clock;
    private final SecurityMailLinkFactory links;
    private final PasswordReplacementEffects replacementEffects;

    @Transactional
    public long request(String email, String source, Locale locale) {
        String normalized = EmailAddress.normalize(email);
        long retryAfter = limits.acquire(normalized, source);
        if (retryAfter > 0) {
            return retryAfter;
        }
        var found = users.findByEmailIgnoreCase(normalized);
        if (found.isEmpty()) {
            return 0;
        }
        Long id = found.orElseThrow().getId();
        if (accounts.lock(id).isEmpty()) {
            return 0;
        }
        var current = users.findCredentialSnapshot(id);
        if (current.isEmpty() || !current.orElseThrow().email().equalsIgnoreCase(normalized)) {
            return 0;
        }
        String token = codec.generate();
        tokens.replace(id, codec.hash(token), clock.instant().plusSeconds(1800));
        events.publishEvent(new PasswordResetMail(normalized, links.passwordReset(token), locale));
        return 0;
    }

    @Transactional
    public void reset(@NotNull @Valid PasswordResetRequest form) {
        if (!codec.valid(form.getToken())) {
            throw new InvalidPasswordResetException();
        }
        String hash = codec.hash(form.getToken());
        Long id = tokens.owner(hash).orElseThrow(InvalidPasswordResetException::new);
        accounts.lock(id).orElseThrow(InvalidPasswordResetException::new);
        var current =
                users.findCredentialSnapshot(id).orElseThrow(InvalidPasswordResetException::new);
        if (!tokens.consume(id, hash, clock.instant())) {
            throw new InvalidPasswordResetException();
        }
        if (encoder.matches(form.getNewPassword(), current.passwordHash())) {
            throw new PasswordChangeException(PasswordChangeError.NEW_UNCHANGED);
        }
        if (users.replacePassword(id, current.passwordHash(), encoder.encode(form.getNewPassword()))
                != 1) {
            throw new InvalidPasswordResetException();
        }
        replacementEffects.complete(id, current.email());
    }
}
