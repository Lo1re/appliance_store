package com.epam.rd.autocode.assessment.appliances.model.oauth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity @Getter @Table(name = "oauth_identity")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OAuthIdentity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, length = 20) private String provider;
    @Column(nullable = false, length = 255) private String subject;
    @Column(name = "user_id", nullable = false) private Long userId;
    public OAuthIdentity(String subject, Long userId) {
        this.provider = "GOOGLE"; this.subject = subject; this.userId = userId;
    }
    @Override public String toString() { return "OAuthIdentity[id=" + id + "]"; }
}
