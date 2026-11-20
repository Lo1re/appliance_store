package com.epam.rd.autocode.assessment.appliances.security;

import com.epam.rd.autocode.assessment.appliances.repository.UserRepository;
import com.epam.rd.autocode.assessment.appliances.validation.EmailAddress;
import java.util.HashMap;
import java.util.regex.Pattern;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

@Configuration
@ConditionalOnProperty(name = "store.security.auth-mode", havingValue = "inmemory")
@EnableConfigurationProperties(InMemoryAuthenticationProperties.class)
public class InMemoryAuthenticationConfig {
    private static final Pattern BCRYPT = Pattern.compile("\\$2[aby]\\$(0[4-9]|[12][0-9]|3[01])\\$[./A-Za-z0-9]{53}");

    @Bean
    public UserDetailsService inMemoryUserDetailsService(InMemoryAuthenticationProperties properties,
                                                         UserRepository users, Environment environment) {
        if (environment.acceptsProfiles(Profiles.of("prod"))
                || !environment.acceptsProfiles(Profiles.of("local", "test"))) {
            throw new IllegalStateException("In-memory authentication is allowed only in local/test");
        }
        if (properties.getAccounts() == null || properties.getAccounts().isEmpty()) {
            throw new IllegalArgumentException("Configure at least one local in-memory account");
        }
        var credentials = new InMemoryUserDetailsManager();
        var expectedRoles = new HashMap<String, AccountRole>();
        for (var configured : properties.getAccounts()) {
            if (configured == null || configured.getEmail() == null || configured.getEmail().isBlank()
                    || configured.getPasswordHash() == null || !BCRYPT.matcher(configured.getPasswordHash()).matches()) {
                throw new IllegalArgumentException("Local in-memory accounts require an email and a BCrypt hash");
            }
            String email = EmailAddress.normalize(configured.getEmail());
            if (credentials.userExists(email)) throw new IllegalArgumentException("Duplicate local in-memory account");
            if (configured.getRole() != null) {
                users.findByEmailIgnoreCase(email).ifPresent(account -> {
                    if (AccountRole.from(account).filter(configured.getRole()::equals).isEmpty()) {
                        throw new IllegalArgumentException("Configured role differs from the local account role");
                    }
                });
                expectedRoles.put(email, configured.getRole());
            }
            credentials.createUser(User.withUsername(email).password(configured.getPasswordHash())
                    .authorities(java.util.List.of()).build());
        }
        return new InMemoryAccountUserDetailsService(credentials, users, expectedRoles);
    }
}
