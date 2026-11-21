package com.epam.rd.autocode.assessment.appliances.repository;

import com.epam.rd.autocode.assessment.appliances.model.User;
import com.epam.rd.autocode.assessment.appliances.repository.projection.CredentialSnapshot;
import java.util.Optional;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public interface UserRepository extends JpaRepository<User, Long> {
    // Lock the base row directly: a polymorphic JOINED query may contain nullable outer joins.
    @Transactional(propagation = Propagation.MANDATORY)
    @Query(value = "select id from users where id = :id for update", nativeQuery = true)
    Optional<Long> lockIdentity(@Param("id") Long id);

    @Transactional(propagation = Propagation.MANDATORY)
    @Query(value = "select id from users where id in (select user_id from employee) order by id for update", nativeQuery = true)
    List<Long> lockEmployeeIdentities();



    @Query("select new com.epam.rd.autocode.assessment.appliances.repository.projection.CredentialSnapshot(u.email, u.password) from User u where u.id = :id")
    Optional<CredentialSnapshot> findCredentialSnapshot(@Param("id") Long id);

    @Transactional(propagation = Propagation.MANDATORY)
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update User u set u.password = :replacement where u.id = :id and u.password = :expected")
    int replacePassword(@Param("id") Long id, @Param("expected") String expected, @Param("replacement") String replacement);

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);
}
