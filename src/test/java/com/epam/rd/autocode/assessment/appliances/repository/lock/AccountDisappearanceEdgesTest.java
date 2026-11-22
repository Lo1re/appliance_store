package com.epam.rd.autocode.assessment.appliances.repository.lock;
import com.epam.rd.autocode.assessment.appliances.repository.UserRepository;
import jakarta.persistence.EntityManager;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
class AccountDisappearanceEdgesTest {@Test void disappearingEntityAfterTheIdentityLockIsAnAbsentAccount(){var users=mock(UserRepository.class);when(users.lockIdentity(1L)).thenReturn(Optional.of(1L));var entities=mock(EntityManager.class);assertThat(new AccountRowLock(users,entities).lock(1L)).isEmpty();verify(entities,never()).refresh(any());}}
