package com.epam.rd.autocode.assessment.appliances.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Persistent credential metadata only; raw refresh values are never stored here. */
@Getter
@Entity
@Table(name = "refresh_token")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "user_id", nullable = false)
    private Long userId;
    @Column(name = "token_hash", nullable = false, length = 64, unique = true)
    private String tokenHash;
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;
    @Column(name = "revoked_at")
    private Instant revokedAt;
    @Column(name = "replaced_by")
    private Long replacedBy;

    public RefreshToken(Long userId, String tokenHash, Instant expiresAt) {
        this.userId = userId; this.tokenHash = tokenHash; this.expiresAt = expiresAt;
    }
    public boolean expiredAt(Instant now) { return !expiresAt.isAfter(now); }
    public void revoke(Instant now) { if (revokedAt == null) revokedAt = now; }
    public void replace(Instant now, Long nextId) { revoke(now); replacedBy = nextId; }
}
