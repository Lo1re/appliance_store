package com.epam.rd.autocode.assessment.appliances.security.authentication;

import com.epam.rd.autocode.assessment.appliances.model.User;
import com.epam.rd.autocode.assessment.appliances.repository.UserRepository;
import com.epam.rd.autocode.assessment.appliances.security.identity.AccountRole;
import com.epam.rd.autocode.assessment.appliances.security.identity.StorePrincipal;
import com.epam.rd.autocode.assessment.appliances.validation.EmailAddress;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnProperty(name = "store.security.auth-mode", havingValue = "database", matchIfMissing = true)
@RequiredArgsConstructor
public class DatabaseUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new UsernameNotFoundException("Account not found");
        }

        String email = EmailAddress.normalize(username);

        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() ->
                        new UsernameNotFoundException("Account not found")
                );

        AccountRole role = AccountRole.from(user)
                .orElseThrow(() ->
                        new UsernameNotFoundException("Account not found")
                );

        return new StorePrincipal(
                user.getId(),
                user.getEmail(),
                user.getPassword(),
                role
        );
    }
}
