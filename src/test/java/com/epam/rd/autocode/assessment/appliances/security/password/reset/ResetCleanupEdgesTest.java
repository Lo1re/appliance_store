package com.epam.rd.autocode.assessment.appliances.security.password.reset;

import static org.mockito.Mockito.*;

import com.epam.rd.autocode.assessment.appliances.repository.password.PasswordResetRepository;

import org.junit.jupiter.api.Test;

import java.time.Clock;

class ResetCleanupEdgesTest {
    @Test
    void logsNonzeroCleanupAndToleratesAnEmptyBatch() {
        var tokens = mock(PasswordResetRepository.class);
        when(tokens.deleteExpired(any())).thenReturn(1, 0);
        var cleanup = new PasswordResetCleanup(tokens, Clock.systemUTC());
        cleanup.clean();
        cleanup.clean();
        verify(tokens, times(2)).deleteExpired(any());
    }
}
