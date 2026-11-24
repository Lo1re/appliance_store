package com.epam.rd.autocode.assessment.appliances.security.authentication;

import com.epam.rd.autocode.assessment.appliances.repository.UserRepository;
import com.epam.rd.autocode.assessment.appliances.security.identity.AccountRole;
import com.epam.rd.autocode.assessment.appliances.validation.normalization.EmailAddress;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

import java.util.HashMap;

@Configuration
@ConditionalOnProperty(name = "store.security.auth-mode", havingValue = "inmemory")
@EnableConfigurationProperties(InMemoryAuthenticationProperties.class)
public class InMemoryAuthenticationConfig {
    @Bean
    public UserDetailsService inMemoryUserDetailsService(
            InMemoryAuthenticationProperties properties,
            UserRepository users,
            Environment environment) {
        new InMemoryAuthenticationValidator(users).validate(properties, environment);
        var credentials = new InMemoryUserDetailsManager();
        var expectedRoles = new HashMap<String, AccountRole>();
        for (var configured : properties.getAccounts()) {
            String email = EmailAddress.normalize(configured.getEmail());
            if (configured.getRole() != null) {
                expectedRoles.put(email, configured.getRole());
            }
            credentials.createUser(
                    User.withUsername(email)
                            .password(configured.getPasswordHash())
                            .authorities(java.util.List.of())
                            .build());
        }
        return new InMemoryAccountUserDetailsService(credentials, users, expectedRoles);
    }
}
