package com.epam.rd.autocode.assessment.appliances.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.Persistable;

@Entity
@Table(name = "login_attempt")
@Getter
@Setter
@NoArgsConstructor
public class LoginAttempt implements Persistable<String> {
    @Transient
    private boolean newRecord = true;

    public LoginAttempt(String email, int failedCount, Instant firstFailedAt, Instant lockedUntil) {
        this.email = email; this.failedCount = failedCount;
        this.firstFailedAt = firstFailedAt; this.lockedUntil = lockedUntil;
    }

    @Override public String getId() { return email; }
    @Override public boolean isNew() { return newRecord; }
    @PostLoad @PostPersist
    private void persisted() { newRecord = false; }

    @Id
    @Column(length = 254)
    private String email;
    @Column(nullable = false)
    private int failedCount;
    @Column(nullable = false)
    private Instant firstFailedAt;
    private Instant lockedUntil;
}
