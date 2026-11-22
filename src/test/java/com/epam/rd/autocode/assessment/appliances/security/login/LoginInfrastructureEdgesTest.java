package com.epam.rd.autocode.assessment.appliances.security.login;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.*;
import org.springframework.security.authentication.event.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
class LoginInfrastructureEdgesTest {
 @Test void counterPersistenceFailuresCannotAuthenticateSilently(){
  var service=mock(LoginAttemptService.class);var events=new LoginAttemptEvents(service);var login=UsernamePasswordAuthenticationToken.unauthenticated("name","secret");
  doThrow(new IllegalStateException("private")).when(service).failed("name");
  assertThatThrownBy(()->events.failed(new AuthenticationFailureBadCredentialsEvent(login,new BadCredentialsException("bad")))).isInstanceOf(AuthenticationServiceException.class).hasMessage("Authentication temporarily unavailable");
  doThrow(new IllegalStateException("private")).when(service).succeeded("name");
  assertThatThrownBy(()->events.succeeded(new AuthenticationSuccessEvent(login))).isInstanceOf(AuthenticationServiceException.class);
 }
 @Test void cleanupSurvivesStorageFailureAndTheProviderRejectsOverlongCredentials(){
  var attempts=mock(LoginAttemptService.class);var cleanup=new LoginAttemptCleanup(attempts);when(attempts.cleanup()).thenReturn(3).thenThrow(new IllegalStateException("private"));cleanup.cleanExpiredCounters();cleanup.cleanExpiredCounters();verify(attempts,times(2)).cleanup();
  var delegate=mock(AuthenticationProvider.class);when(attempts.remainingLock("name")).thenReturn(Duration.ZERO);var provider=new LoginProtectedAuthenticationProvider(delegate,attempts);
  assertThatThrownBy(()->provider.authenticate(UsernamePasswordAuthenticationToken.unauthenticated("name","x".repeat(73)))).isInstanceOf(BadCredentialsException.class);verifyNoInteractions(delegate);
  provider.authenticate(UsernamePasswordAuthenticationToken.unauthenticated("name",new Object()));verify(delegate).authenticate(any());
 }
}
