package com.epam.rd.autocode.assessment.appliances.security.jwt.access;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.epam.rd.autocode.assessment.appliances.model.security.AccountSecurityState;
import com.epam.rd.autocode.assessment.appliances.repository.security.AccountSecurityStateRepository;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;

class AccessTokenVersionServiceTest {
    @Test
    void rejectsOldVersionsAndPreservesCurrentVersion() {
        var states = mock(AccountSecurityStateRepository.class);
        var state = new AccountSecurityState(1L, 2);
        when(states.findById(1L)).thenReturn(Optional.of(state));
        var service = new AccessTokenVersionService(states);
        assertThatCode(() -> service.requireCurrent(1L, 2)).doesNotThrowAnyException();
        service.revokePrevious(1L);
        assertThatThrownBy(() -> service.requireCurrent(1L, 2))
                .isInstanceOf(AccessDeniedException.class);
        assertThatCode(() -> service.requireCurrent(1L, 3)).doesNotThrowAnyException();
    }
}
