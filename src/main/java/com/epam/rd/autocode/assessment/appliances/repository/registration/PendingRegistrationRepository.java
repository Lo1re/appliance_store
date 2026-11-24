package com.epam.rd.autocode.assessment.appliances.repository.registration;

import com.epam.rd.autocode.assessment.appliances.model.registration.PendingRegistration;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface PendingRegistrationRepository extends JpaRepository<PendingRegistration, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select pending from PendingRegistration pending where pending.tokenHash = :hash")
    Optional<PendingRegistration> lockByHash(@Param("hash") String hash);

    @Modifying
    @Query("delete from PendingRegistration pending where pending.expiresAt <= :now")
    int deleteExpired(@Param("now") Instant now);
}
