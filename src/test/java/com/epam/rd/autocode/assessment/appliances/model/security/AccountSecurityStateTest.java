package com.epam.rd.autocode.assessment.appliances.model.security;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;

class AccountSecurityStateTest {
    @Test
    void credentialVersionAdvancesAndNeverWrapsOnOverflow() {
        var state = new AccountSecurityState(7L, 3L);
        state.advance();
        assertThat(state.getCredentialVersion()).isEqualTo(4L);
        var exhausted = new AccountSecurityState(7L, Long.MAX_VALUE);
        assertThatThrownBy(exhausted::advance).isInstanceOf(ArithmeticException.class);
        assertThat(exhausted.getCredentialVersion()).isEqualTo(Long.MAX_VALUE);
    }
}
