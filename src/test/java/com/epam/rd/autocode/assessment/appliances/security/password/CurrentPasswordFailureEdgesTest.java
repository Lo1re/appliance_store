package com.epam.rd.autocode.assessment.appliances.security.password;
import com.epam.rd.autocode.assessment.appliances.security.login.LoginAttemptService;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.authentication.AuthenticationServiceException;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
class CurrentPasswordFailureEdgesTest {
 @Test void missingAndOversizedSecretsCountAsFailedAttempts(){var encoder=mock(PasswordEncoder.class);var attempts=mock(LoginAttemptService.class);when(attempts.remainingLock("email")).thenReturn(Duration.ZERO);var verifier=new CurrentPasswordVerifier(encoder,attempts);for(String raw:new String[]{null,"x".repeat(73)})assertThatThrownBy(()->verifier.verify(1L,"email",raw,"hash")).isInstanceOf(com.epam.rd.autocode.assessment.appliances.exception.password.PasswordChangeException.class);verify(attempts,times(2)).failed("email");verifyNoInteractions(encoder);}
 @Test void storageFailureIsSanitized(){var attempts=mock(LoginAttemptService.class);when(attempts.remainingLock("email")).thenThrow(new DataAccessResourceFailureException("private"));var verifier=new CurrentPasswordVerifier(mock(PasswordEncoder.class),attempts);assertThatThrownBy(()->verifier.verify(1L,"email","secret","hash")).isInstanceOf(AuthenticationServiceException.class).hasMessage("Authentication temporarily unavailable");}
}
