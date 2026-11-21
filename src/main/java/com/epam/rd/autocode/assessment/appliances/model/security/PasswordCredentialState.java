package com.epam.rd.autocode.assessment.appliances.model.security;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity @Getter @Table(name = "password_credential_state")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PasswordCredentialState {
    @Id @Column(name = "user_id") private Long userId;
    @Column(name = "password_set", nullable = false) private boolean passwordSet;
    public PasswordCredentialState(Long userId, boolean passwordSet) { this.userId = userId; this.passwordSet = passwordSet; }
    public void markSet() { passwordSet = true; }
}
