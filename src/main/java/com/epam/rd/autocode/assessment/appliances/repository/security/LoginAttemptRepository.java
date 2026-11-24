package com.epam.rd.autocode.assessment.appliances.repository.security;

import com.epam.rd.autocode.assessment.appliances.model.security.LoginAttempt;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface LoginAttemptRepository extends JpaRepository<LoginAttempt, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from LoginAttempt a where a.email = :email")
    Optional<LoginAttempt> findLocked(@Param("email") String email);

    @Modifying
    @Query(
            "delete from LoginAttempt a where a.firstFailedAt < :cutoff and (a.lockedUntil is null"
                    + " or a.lockedUntil <= :now)")
    int deleteExpired(@Param("cutoff") Instant cutoff, @Param("now") Instant now);
}
