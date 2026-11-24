package com.epam.rd.autocode.assessment.appliances.model.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PasswordCredentialStateTest {
    @Test
    void markingPasswordSetIsIdempotent() {
        var state = new PasswordCredentialState(7L, false);
        assertThat(state.isPasswordSet()).isFalse();
        state.markSet();
        state.markSet();
        assertThat(state.isPasswordSet()).isTrue();
    }
}
