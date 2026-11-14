package com.epam.rd.autocode.assessment.appliances.security;

import com.epam.rd.autocode.assessment.appliances.model.Client;
import com.epam.rd.autocode.assessment.appliances.model.Employee;
import com.epam.rd.autocode.assessment.appliances.model.User;
import com.epam.rd.autocode.assessment.appliances.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class DatabaseUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new UsernameNotFoundException("Account not found");
        }

        String email = username.strip().toLowerCase(Locale.ROOT);

        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() ->
                        new UsernameNotFoundException("Account not found")
                );

        return new StorePrincipal(
                user.getId(),
                user.getEmail(),
                user.getPassword(),
                roleOf(user)
        );
    }

    private String roleOf(User user) {
        if (user instanceof Client) {
            return "CLIENT";
        }

        if (user instanceof Employee) {
            return "EMPLOYEE";
        }

        throw new UsernameNotFoundException("Account not found");
    }
}