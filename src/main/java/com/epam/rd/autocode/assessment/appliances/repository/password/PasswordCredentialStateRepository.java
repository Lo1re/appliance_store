package com.epam.rd.autocode.assessment.appliances.repository.password;

import com.epam.rd.autocode.assessment.appliances.model.security.PasswordCredentialState;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PasswordCredentialStateRepository
        extends JpaRepository<PasswordCredentialState, Long> {}
