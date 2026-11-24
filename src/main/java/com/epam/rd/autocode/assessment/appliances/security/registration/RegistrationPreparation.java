package com.epam.rd.autocode.assessment.appliances.security.registration;

import com.epam.rd.autocode.assessment.appliances.dto.registration.ClientRegistrationRequest;
import com.epam.rd.autocode.assessment.appliances.exception.AccountError;
import com.epam.rd.autocode.assessment.appliances.exception.AccountException;
import com.epam.rd.autocode.assessment.appliances.mail.SecurityMailLinkFactory;
import com.epam.rd.autocode.assessment.appliances.repository.ClientRepository;
import com.epam.rd.autocode.assessment.appliances.repository.UserRepository;
import com.epam.rd.autocode.assessment.appliances.security.password.reset.ResetRequestLimiter;
import com.epam.rd.autocode.assessment.appliances.security.password.reset.ResetTokenCodec;
import com.epam.rd.autocode.assessment.appliances.service.policy.ClientCardPolicy;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import lombok.RequiredArgsConstructor;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.time.Clock;

// Validates/admission-limits signup and hashes the password before any temporary storage.
@Component
@Validated
@RequiredArgsConstructor
@ConditionalOnClientRegistration
public class RegistrationPreparation {
    private final UserRepository users;
    private final ClientRepository clients;
    private final PasswordEncoder encoder;
    private final ResetTokenCodec tokens;
    private final ResetRequestLimiter limits;
    private final SecurityMailLinkFactory links;
    private final Clock clock;

    public Result prepare(@NotNull @Valid ClientRegistrationRequest form, String source) {
        long retry = limits.acquire("registration:" + form.getEmail(), "registration:" + source);
        if (retry > 0) {
            return new Result(retry, null);
        }
        // Keep the same expensive password work for valid new/existing-email submissions.
        String hash = encoder.encode(form.getNewPassword());
        if (users.existsByEmailIgnoreCase(form.getEmail())) {
            throw new AccountException(AccountError.EMAIL_DUPLICATE);
        }
        new ClientCardPolicy(clients).requireAvailable(form.getCard(), null);
        String token = tokens.generate();
        var details =
                new RegistrationDetails(form.getName(), form.getEmail(), form.getCard(), hash);
        return new Result(
                0,
                new RegistrationChallenge(
                        details,
                        tokens.hash(token),
                        links.registration(token),
                        clock.instant().plusSeconds(1800)));
    }

    public record Result(long retryAfter, RegistrationChallenge challenge) {
        @Override
        public String toString() {
            return "RegistrationPreparationResult[redacted]";
        }
    }
}
