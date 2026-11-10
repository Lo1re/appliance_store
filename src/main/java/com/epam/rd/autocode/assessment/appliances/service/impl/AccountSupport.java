package com.epam.rd.autocode.assessment.appliances.service.impl;

import com.epam.rd.autocode.assessment.appliances.exception.AccountError;
import com.epam.rd.autocode.assessment.appliances.exception.AccountException;
import com.epam.rd.autocode.assessment.appliances.model.User;
import com.epam.rd.autocode.assessment.appliances.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AccountSupport {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    void requireAvailableEmail(String email, Long id) {
        boolean emailTaken = id != null
                ? userRepository.existsByEmailIgnoreCaseAndIdNot(email, id)
                : userRepository.existsByEmailIgnoreCase(email);

        if (emailTaken) {
            throw new AccountException(AccountError.EMAIL_DUPLICATE);
        }
    }

    void setPassword(User user, String password) {
        user.setPassword(passwordEncoder.encode(password));
    }
}