package com.epam.rd.autocode.assessment.appliances.repository;

import com.epam.rd.autocode.assessment.appliances.model.RefreshToken;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    // Scalar lookup avoids caching an entity before its user's serialization lock is acquired.
    @Query("select t.userId from RefreshToken t where t.tokenHash = :hash")
    Optional<Long> findOwnerByHash(@Param("hash") String hash);
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update RefreshToken t set t.revokedAt = :now where t.userId = :userId and t.revokedAt is null")
    int revokeAll(@Param("userId") Long userId, @Param("now") Instant now);

    @Query("select distinct t.userId from RefreshToken t where t.expiresAt < :cutoff order by t.userId")
    List<Long> findExpiredOwners(@Param("cutoff") Instant cutoff, Pageable pageable);

    @Modifying
    @Query("delete from RefreshToken t where t.userId = :userId and t.expiresAt < :cutoff")
    int deleteExpired(@Param("userId") Long userId, @Param("cutoff") Instant cutoff);
}
