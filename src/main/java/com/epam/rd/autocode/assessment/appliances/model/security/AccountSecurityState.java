package com.epam.rd.autocode.assessment.appliances.model.security;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity @Getter @Table(name = "account_security_state")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AccountSecurityState {
    @Id @Column(name = "user_id") private Long userId;
    @Column(name = "credential_version", nullable = false) private long credentialVersion;
    public AccountSecurityState(Long userId, long credentialVersion) { this.userId = userId; this.credentialVersion = credentialVersion; }
    public void advance() { credentialVersion = Math.incrementExact(credentialVersion); }
}
