package com.epam.rd.autocode.assessment.appliances.repository;

import com.epam.rd.autocode.assessment.appliances.model.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public interface UserRepository extends JpaRepository<User, Long> {
    // Lock the base row directly: a polymorphic JOINED query may contain nullable outer joins.
    @Transactional(propagation = Propagation.MANDATORY)
    @Query(value = "select id from users where id = :id for update", nativeQuery = true)
    Optional<Long> lockIdentity(@Param("id") Long id);


    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);
}
