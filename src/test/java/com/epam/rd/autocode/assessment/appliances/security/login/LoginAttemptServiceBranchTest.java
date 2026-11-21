package com.epam.rd.autocode.assessment.appliances.security.login;
import com.epam.rd.autocode.assessment.appliances.model.LoginAttempt;
import com.epam.rd.autocode.assessment.appliances.repository.LoginAttemptRepository;
import java.time.*;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
class LoginAttemptServiceBranchTest {
    final LoginAttemptRepository attempts=mock(LoginAttemptRepository.class);
    final LoginProtectionProperties properties=new LoginProtectionProperties(5,Duration.ofMinutes(15),Duration.ofMinutes(15),Duration.ofDays(1),3600000);
    final Instant now=Instant.parse("2026-11-22T00:00:00Z");
    final LoginAttemptService service=new LoginAttemptService(attempts,new LoginAttemptPolicy(properties),properties,Clock.fixed(now,ZoneOffset.UTC),manager());
    PlatformTransactionManager manager() {var manager=mock(PlatformTransactionManager.class);when(manager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());return manager;}
    @Test void invalidIdentifiersNeverReachPersistence() {for(String name:new String[]{null," ","x".repeat(255)}) {assertThat(service.remainingLock(name)).isZero();service.failed(name);service.succeeded(name);}verifyNoInteractions(attempts);}
    @Test void successfulLoginDoesNotDeleteALiveLock() {var attempt=new LoginAttempt("user@example.test",5,now,now.plusSeconds(60));when(attempts.findLocked(attempt.getEmail())).thenReturn(Optional.of(attempt));service.succeeded(attempt.getEmail());verify(attempts,never()).delete(any());}
    @Test void retriesConcurrentInitialInsertThenSucceeds() {when(attempts.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("race")).thenAnswer(inv->inv.getArgument(0));when(attempts.existsById("user@example.test")).thenReturn(true);service.failed("user@example.test");verify(attempts,times(2)).saveAndFlush(any());}
    @Test void missingCompetingInsertDoesNotHideFailure() {when(attempts.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("race"));assertThatThrownBy(()->service.failed("user@example.test")).isInstanceOf(DataIntegrityViolationException.class);verify(attempts,times(1)).saveAndFlush(any());}
    @Test void retriesAreBoundedAtThreeAttempts() {when(attempts.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("race"));when(attempts.existsById("user@example.test")).thenReturn(true);assertThatThrownBy(()->service.failed("user@example.test")).isInstanceOf(DataIntegrityViolationException.class);verify(attempts,times(3)).saveAndFlush(any());}
}
