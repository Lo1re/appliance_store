package com.epam.rd.autocode.assessment.appliances.repository.password;

import com.epam.rd.autocode.assessment.appliances.model.security.PasswordResetToken;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

public interface PasswordResetRepository extends JpaRepository<PasswordResetToken, Long> {
    @Transactional(readOnly = true)
    @Query("select token.userId from PasswordResetToken token where token.tokenHash = :hash")
    Optional<Long> owner(@Param("hash") String hash);

    // Caller holds the account lock; removal and insertion share that transaction.
    @Transactional(propagation = Propagation.MANDATORY)
    default void replace(Long userId, String hash, Instant expires) {
        deleteAll(userId);
        saveAndFlush(new PasswordResetToken(userId, hash, expires));
    }

    // A single conditional DELETE consumes a live credential at most once.
    @Transactional(propagation = Propagation.MANDATORY)
    default boolean consume(Long userId, String hash, Instant now) {
        return deleteLive(userId, hash, now) == 1;
    }

    @Modifying(flushAutomatically = true)
    @Transactional(propagation = Propagation.MANDATORY)
    @Query(
            "delete from PasswordResetToken token where token.userId = :userId and token.tokenHash"
                    + " = :hash and token.expiresAt > :now")
    int deleteLive(
            @Param("userId") Long userId, @Param("hash") String hash, @Param("now") Instant now);

    @Modifying(flushAutomatically = true)
    @Transactional(propagation = Propagation.MANDATORY)
    @Query("delete from PasswordResetToken token where token.userId = :userId")
    void deleteAll(@Param("userId") Long userId);

    @Modifying(flushAutomatically = true)
    @Transactional
    @Query("delete from PasswordResetToken token where token.expiresAt <= :now")
    int deleteExpired(@Param("now") Instant now);
}
