package com.epam.rd.autocode.assessment.appliances.security.login;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class LoginProtectedAuthenticationProviderTest {
    private final AuthenticationProvider delegate = mock(AuthenticationProvider.class);
    private final LoginAttemptService attempts = mock(LoginAttemptService.class);
    private final LoginProtectedAuthenticationProvider provider = new LoginProtectedAuthenticationProvider(delegate, attempts);
    private final UsernamePasswordAuthenticationToken input = UsernamePasswordAuthenticationToken.unauthenticated("user@example.com", "password");

    @Test
    void blockedAttemptNeverReachesPasswordVerification() {
        when(attempts.remainingLock("user@example.com")).thenReturn(Duration.ofSeconds(1));
        assertThatThrownBy(() -> provider.authenticate(input)).isInstanceOf(LockedException.class);
        verifyNoInteractions(delegate);
    }

    @Test
    void checksFreshLockAgainAfterPasswordVerification() {
        when(attempts.remainingLock("user@example.com")).thenReturn(Duration.ZERO, Duration.ofSeconds(1));
        when(delegate.authenticate(input)).thenReturn(input);
        assertThatThrownBy(() -> provider.authenticate(input)).isInstanceOf(LockedException.class);
        verify(attempts, times(2)).remainingLock("user@example.com");
    }

    @Test
    void providerDoesNotDoubleCountTheFailureHandledByEvents() {
        when(attempts.remainingLock("user@example.com")).thenReturn(Duration.ZERO);
        var failure = new BadCredentialsException("Bad credentials");
        when(delegate.authenticate(input)).thenThrow(failure);
        assertThatThrownBy(() -> provider.authenticate(input)).isSameAs(failure);
        verify(attempts, never()).failed(anyString());
    }
}
