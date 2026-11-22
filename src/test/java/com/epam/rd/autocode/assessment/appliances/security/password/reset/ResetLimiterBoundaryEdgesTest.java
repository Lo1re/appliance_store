package com.epam.rd.autocode.assessment.appliances.security.password.reset;
import java.time.*;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
class ResetLimiterBoundaryEdgesTest {
 @Test void invalidRatePoliciesFailClosed(){int[][] inputs={{0,1,1,0},{1,0,1,0},{1,1,0,0},{1,1,1,-1}};for(int[] c:inputs)assertThatThrownBy(()->new ResetRequestLimiter(Clock.systemUTC(),new ResetTokenCodec(),c[0],c[1],c[2],c[3])).isInstanceOf(IllegalArgumentException.class);}
 @Test void boundedStorageRejectsNewIdentifiersWithoutDroppingExistingLimits(){var limiter=new ResetRequestLimiter(Clock.fixed(Instant.EPOCH,ZoneOffset.UTC),new ResetTokenCodec(),1,1,3600,60);for(int i=0;i<5000;i++)assertThat(limiter.acquire("email"+i,"source"+i)).isZero();assertThat(limiter.acquire("fresh","fresh")).isEqualTo(3600);assertThat(limiter.acquire("email0","fresh")).isEqualTo(3600);assertThat(limiter.acquire("email0","source0")).isEqualTo(3600);}
 @Test void theLaterEmailWindowWinsOverTheSourceWindow(){var clock=mock(Clock.class);when(clock.instant()).thenReturn(Instant.EPOCH,Instant.EPOCH.plusSeconds(10),Instant.EPOCH.plusSeconds(20));var limiter=new ResetRequestLimiter(clock,new ResetTokenCodec(),1,1,3600,0);assertThat(limiter.acquire("first","source")).isZero();assertThat(limiter.acquire("target","other")).isZero();assertThat(limiter.acquire("target","source")).isEqualTo(3590);}
}
