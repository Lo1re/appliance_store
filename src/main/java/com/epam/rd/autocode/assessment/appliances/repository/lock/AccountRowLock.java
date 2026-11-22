package com.epam.rd.autocode.assessment.appliances.repository.lock;

import com.epam.rd.autocode.assessment.appliances.model.User;
import com.epam.rd.autocode.assessment.appliances.repository.UserRepository;
import jakarta.persistence.EntityManager;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Locks the base row, then refreshes cached profile/credential state in the current unit of work. */
@Repository
@RequiredArgsConstructor
public class AccountRowLock {
    private final UserRepository users;
    private final EntityManager entities;
    @Transactional(propagation = Propagation.MANDATORY)
    public void lockEmployeeAccounts() {
        users.lockEmployeeIdentities();
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public Optional<User> lock(Long id) {
        if (users.lockIdentity(id).isEmpty()) {
            return Optional.empty();
        }
        User user = entities.find(User.class, id);
        if (user == null) {
            return Optional.empty();
        }
        entities.refresh(user);
        return Optional.of(user);
    }
}
