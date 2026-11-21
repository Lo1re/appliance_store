package com.epam.rd.autocode.assessment.appliances.security.jwt.access;

import com.epam.rd.autocode.assessment.appliances.model.security.AccountSecurityState;
import com.epam.rd.autocode.assessment.appliances.repository.security.AccountSecurityStateRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

class AccessTokenVersionServiceTest {
    @Test void rejectsOldVersionsAndPreservesCurrentVersion() {
        var states = mock(AccountSecurityStateRepository.class);
        var state = new AccountSecurityState(1L, 2);
        when(states.findById(1L)).thenReturn(Optional.of(state));
        var service = new AccessTokenVersionService(states);
        assertThatCode(() -> service.requireCurrent(1L, 2)).doesNotThrowAnyException();
        service.revokePrevious(1L);
        assertThatThrownBy(() -> service.requireCurrent(1L, 2)).isInstanceOf(AccessDeniedException.class);
        assertThatCode(() -> service.requireCurrent(1L, 3)).doesNotThrowAnyException();
    }
}
