package com.epam.rd.autocode.assessment.appliances.aspect;

import com.epam.rd.autocode.assessment.appliances.exception.AccountError;
import com.epam.rd.autocode.assessment.appliances.exception.AccountException;
import com.epam.rd.autocode.assessment.appliances.exception.CatalogError;
import com.epam.rd.autocode.assessment.appliances.exception.CatalogException;
import com.epam.rd.autocode.assessment.appliances.exception.OrderException;
import jakarta.validation.ConstraintViolationException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(OutputCaptureExtension.class)
class LoggingServicesTest {
    private final LoggingServices aspect = new LoggingServices();
    private final Transactional transaction = mock(Transactional.class);
    private final ProceedingJoinPoint invocation = mock(ProceedingJoinPoint.class);

    @Test
    void readOnlyOperationPassesThroughSilently(CapturedOutput output) throws Throwable {
        when(transaction.readOnly()).thenReturn(true);
        when(invocation.proceed()).thenReturn("result");

        assertThat(aspect.logServiceWrite(invocation, transaction)).isEqualTo("result");
        verify(invocation, never()).getSignature();
        assertThat(output.getOut()).doesNotContain("Service operation");
    }

    @Test
    void logsSuccessWithoutReturningPrivateDataToTheLog(CapturedOutput output) throws Throwable {
        signature();
        when(invocation.proceed()).thenReturn("private-result");

        assertThat(aspect.logServiceWrite(invocation, transaction)).isEqualTo("private-result");
        assertThat(output.getOut()).contains("Service operation completed: LoggingServicesTest.update")
                .doesNotContain("private-result");
    }

    @ParameterizedTest
    @MethodSource("businessFailures")
    void logsRejectionAndRethrowsOriginalFailure(RuntimeException failure, CapturedOutput output) throws Throwable {
        signature();
        when(invocation.proceed()).thenThrow(failure);

        assertThatThrownBy(() -> aspect.logServiceWrite(invocation, transaction)).isSameAs(failure);
        assertThat(output.getOut()).contains("Service operation rejected: LoggingServicesTest.update")
                .contains(failure.getClass().getSimpleName());
    }

    @Test
    void logsUnexpectedFailureWithoutExposingItsMessage(CapturedOutput output) throws Throwable {
        signature();
        var failure = new IllegalStateException("private-failure-message");
        when(invocation.proceed()).thenThrow(failure);

        assertThatThrownBy(() -> aspect.logServiceWrite(invocation, transaction)).isSameAs(failure);
        assertThat(output.getOut()).contains("Service operation failed: LoggingServicesTest.update")
                .doesNotContain("private-failure-message");
    }

    private void signature() {
        var signature = mock(Signature.class);
        when(invocation.getSignature()).thenReturn(signature);
        doReturn(LoggingServicesTest.class).when(signature).getDeclaringType();
        when(signature.getName()).thenReturn("update");
    }

    private static Stream<RuntimeException> businessFailures() {
        return Stream.of(new CatalogException(CatalogError.MANUFACTURER_IN_USE),
                new AccountException(AccountError.CLIENT_IN_USE),
                new OrderException("order.not-found"),
                new ConstraintViolationException(Set.of()));
    }
}
