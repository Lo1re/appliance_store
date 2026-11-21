package com.epam.rd.autocode.assessment.appliances.model.registration;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity @Table(name = "pending_registration") @Getter @NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PendingRegistration {
    @Id @Column(name = "token_hash", length = 64) private String tokenHash;
    @Column(nullable = false, length = 100) private String name;
    @Column(nullable = false, length = 254) private String email;
    @Column(length = 32) private String card;
    @Column(name = "password_hash", nullable = false, length = 255) private String passwordHash;
    @Column(name = "expires_at", nullable = false) private Instant expiresAt;

    public PendingRegistration(String tokenHash, String name, String email, String card, String passwordHash, Instant expiresAt) {
        this.tokenHash = tokenHash; this.name = name; this.email = email;
        this.card = card; this.passwordHash = passwordHash; this.expiresAt = expiresAt;
    }
    @Override public String toString() { return "PendingRegistration[redacted]"; }
}
