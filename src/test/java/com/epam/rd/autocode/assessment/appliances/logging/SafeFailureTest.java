package com.epam.rd.autocode.assessment.appliances.logging;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;

class SafeFailureTest {
    @Test
    void preservesFramesAndCauseTypesWithoutPreservingMessages() {
        var cause = new IllegalArgumentException("private-token");
        var source = new IllegalStateException("private-password", cause);
        Throwable safe = SafeFailure.snapshot(source);
        assertThat(safe.getMessage()).isEqualTo(IllegalStateException.class.getName());
        assertThat(safe.getStackTrace()).containsExactly(source.getStackTrace());
        assertThat(safe.getCause().getMessage())
                .isEqualTo(IllegalArgumentException.class.getName());
        assertThat(safe.getCause().getCause()).isNull();
        assertThat(source.getMessage()).isEqualTo("private-password");
    }

    @Test
    void handlesNullAndCyclicCauses() {
        assertThat(SafeFailure.snapshot(null)).isNull();
        var first = new RuntimeException("first-secret");
        var second = new RuntimeException("second-secret");
        first.initCause(second);
        second.initCause(first);
        var safe = SafeFailure.snapshot(first);
        assertThat(safe.getCause()).isNotNull();
        assertThat(safe.getCause().getCause()).isNull();
    }
}
