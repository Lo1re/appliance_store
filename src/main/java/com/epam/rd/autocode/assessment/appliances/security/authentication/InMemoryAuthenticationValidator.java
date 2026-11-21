package com.epam.rd.autocode.assessment.appliances.security.authentication;

import com.epam.rd.autocode.assessment.appliances.repository.UserRepository;
import com.epam.rd.autocode.assessment.appliances.security.identity.AccountRole;
import com.epam.rd.autocode.assessment.appliances.validation.EmailAddress;
import java.util.HashSet;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

/** Checks local authentication settings without constructing a provider or exposing values. */
@RequiredArgsConstructor
public class InMemoryAuthenticationValidator {
    private static final Pattern BCRYPT = Pattern.compile("\\$2[aby]\\$(0[4-9]|[12][0-9]|3[01])\\$[./A-Za-z0-9]{53}");
    private final UserRepository users;

    public void validate(InMemoryAuthenticationProperties properties, Environment environment) {
        if (environment.acceptsProfiles(Profiles.of("prod"))
                || !environment.acceptsProfiles(Profiles.of("local", "test"))) {
            throw new IllegalStateException("In-memory authentication is allowed only in local/test");
        }
        if (properties.getAccounts() == null || properties.getAccounts().isEmpty()) {
            throw new IllegalArgumentException("Configure at least one local in-memory account");
        }
        var seen = new HashSet<String>();
        for (var configured : properties.getAccounts()) {
            if (configured == null || configured.getEmail() == null || configured.getEmail().isBlank()
                    || configured.getPasswordHash() == null || !BCRYPT.matcher(configured.getPasswordHash()).matches()) {
                throw new IllegalArgumentException("Local in-memory accounts require an email and a BCrypt hash");
            }
            String email = EmailAddress.normalize(configured.getEmail());
            if (!seen.add(email)) throw new IllegalArgumentException("Duplicate local in-memory account");
            if (configured.getRole() != null) {
                users.findByEmailIgnoreCase(email).ifPresent(account -> {
                    if (AccountRole.from(account).filter(configured.getRole()::equals).isEmpty()) {
                        throw new IllegalArgumentException("Configured role differs from the local account role");
                    }
                });
            }
        }
    }
}
