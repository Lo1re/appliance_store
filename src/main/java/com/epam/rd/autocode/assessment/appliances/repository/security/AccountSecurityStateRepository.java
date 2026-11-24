package com.epam.rd.autocode.assessment.appliances.repository.security;

import com.epam.rd.autocode.assessment.appliances.model.security.AccountSecurityState;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountSecurityStateRepository extends JpaRepository<AccountSecurityState, Long> {}
