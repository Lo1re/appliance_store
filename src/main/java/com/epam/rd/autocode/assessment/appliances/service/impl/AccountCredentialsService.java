package com.epam.rd.autocode.assessment.appliances.service.impl;

import com.epam.rd.autocode.assessment.appliances.exception.AccountError;
import com.epam.rd.autocode.assessment.appliances.exception.AccountException;
import com.epam.rd.autocode.assessment.appliances.model.User;
import com.epam.rd.autocode.assessment.appliances.repository.UserRepository;
import com.epam.rd.autocode.assessment.appliances.validation.EmailAddress;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AccountCredentialsService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    void assignEmail(User user, String email) {
        String normalized = EmailAddress.normalize(email);
        boolean taken = user.getId() == null
                ? userRepository.existsByEmailIgnoreCase(normalized)
                : userRepository.existsByEmailIgnoreCaseAndIdNot(normalized, user.getId());
        if (taken) {
            throw new AccountException(AccountError.EMAIL_DUPLICATE);
        }
        user.setEmail(normalized);
    }

    void setPassword(User user, String password) {
        user.setPassword(passwordEncoder.encode(password));
    }
}
