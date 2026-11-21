package com.epam.rd.autocode.assessment.appliances.security.registration;

import com.epam.rd.autocode.assessment.appliances.exception.AccountError;
import com.epam.rd.autocode.assessment.appliances.exception.AccountException;
import com.epam.rd.autocode.assessment.appliances.logging.BusinessAction;
import com.epam.rd.autocode.assessment.appliances.logging.BusinessAudit;
import com.epam.rd.autocode.assessment.appliances.mail.SecurityNotificationMail;
import com.epam.rd.autocode.assessment.appliances.model.Client;
import com.epam.rd.autocode.assessment.appliances.repository.ClientRepository;
import com.epam.rd.autocode.assessment.appliances.repository.UserRepository;
import com.epam.rd.autocode.assessment.appliances.security.password.PasswordCredentialStatusService;
import com.epam.rd.autocode.assessment.appliances.persistence.PersistenceBoundary;
import com.epam.rd.autocode.assessment.appliances.persistence.PersistenceOperation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

/** Anonymous, verified signup boundary; it can construct only a Client, never an Employee. */
@Component @Validated @RequiredArgsConstructor @ConditionalOnClientRegistration
public class ClientRegistrationService {
    private final UserRepository users;
    private final ClientRepository clients;
    private final PasswordCredentialStatusService passwords;
    private final BusinessAudit audit;
    private final ApplicationEventPublisher events;
    @Transactional
    @PersistenceBoundary(PersistenceOperation.ACCOUNT_WRITE)
    public Long register(@NotNull @Valid RegistrationDetails details) {
        if (users.existsByEmailIgnoreCase(details.email())) throw new AccountException(AccountError.EMAIL_DUPLICATE);
        if (details.card() != null && clients.existsByCard(details.card())) throw new AccountException(AccountError.CARD_DUPLICATE);
        Client client = clients.saveAndFlush(new Client(null, details.name(), details.email(), details.passwordHash(), details.card()));
        passwords.rememberExistingPassword(client.getId());
        audit.record(BusinessAction.CLIENT_CREATED, client.getId());
        events.publishEvent(new SecurityNotificationMail(details.email(), SecurityNotificationMail.Kind.ACCOUNT_CREATED, LocaleContextHolder.getLocale()));
        return client.getId();
    }
}
