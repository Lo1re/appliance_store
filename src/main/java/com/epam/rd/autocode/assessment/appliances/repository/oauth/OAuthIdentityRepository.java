package com.epam.rd.autocode.assessment.appliances.repository.oauth;

import com.epam.rd.autocode.assessment.appliances.model.oauth.OAuthIdentity;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OAuthIdentityRepository extends JpaRepository<OAuthIdentity, Long> {
    Optional<OAuthIdentity> findByProviderAndSubject(String provider, String subject);

    boolean existsByProviderAndUserId(String provider, Long userId);
}
