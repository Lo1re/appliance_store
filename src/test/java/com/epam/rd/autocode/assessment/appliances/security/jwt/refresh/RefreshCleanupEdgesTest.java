package com.epam.rd.autocode.assessment.appliances.security.jwt.refresh;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;
class RefreshCleanupEdgesTest {
 @Test void cleanupDoesNotKillTheSchedulerAfterAStorageFailure(){var tokens=mock(RefreshTokenService.class);when(tokens.cleanup()).thenReturn(2).thenThrow(new IllegalStateException("private"));var cleanup=new RefreshTokenCleanup(tokens);cleanup.cleanup();cleanup.cleanup();verify(tokens,times(2)).cleanup();}
}
