package com.epam.rd.autocode.assessment.appliances.security.jwt.access;
import com.epam.rd.autocode.assessment.appliances.security.identity.*;
import com.epam.rd.autocode.assessment.appliances.security.jwt.config.JwtProperties;
import java.time.*;import java.util.*;import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
class IdentityAndClaimEdgesTest {
 @Test void identitiesUseStableAccountIdsAndExposeNoCredential(){var p=new AccessTokenPrincipal(7L);assertThat(p.equals("other")).isFalse();assertThat(p.equals(new AccessTokenPrincipal(8L))).isFalse();assertThat(p.equals(new AccessTokenPrincipal(7L))).isTrue();var auth=new AccessTokenAuthentication(p,List.of(),Instant.EPOCH);assertThat(auth.getCredentials()).isEqualTo("");var store=new StorePrincipal(null,"name","hash",AccountRole.CLIENT);assertThat(store.equals(new Object())).isFalse();assertThat(store.hashCode()).isEqualTo(System.identityHashCode(store));assertThat(new StorePrincipal(7L,"name","hash",AccountRole.CLIENT).equals(new Object())).isFalse();}
 void invalid(Consumer<Jwt> mutate){var jwt=mock(Jwt.class);var now=Instant.parse("2026-01-01T00:00:00Z");when(jwt.getClaims()).thenReturn(Map.of("roles",List.of("CLIENT")));when(jwt.getSubject()).thenReturn("7");when(jwt.getAudience()).thenReturn(List.of("appliance-store-api"));when(jwt.getIssuedAt()).thenReturn(now);when(jwt.getExpiresAt()).thenReturn(now.plusSeconds(100));when(jwt.getId()).thenReturn("id");mutate.accept(jwt);assertThat(new AccessTokenClaimsValidator(new JwtProperties(),Clock.fixed(now,ZoneOffset.UTC)).validate(jwt).hasErrors()).isTrue();}
 @Test void missingOrInconsistentClaimsAreRejected(){
  invalid(j->when(j.getSubject()).thenReturn(null));invalid(j->when(j.getSubject()).thenReturn("bad"));invalid(j->when(j.getSubject()).thenReturn("9999999999999999999"));invalid(j->when(j.getAudience()).thenReturn(null));invalid(j->when(j.getAudience()).thenReturn(List.of("other")));
  invalid(j->when(j.getIssuedAt()).thenReturn(null));invalid(j->when(j.getExpiresAt()).thenReturn(null));invalid(j->when(j.getExpiresAt()).thenReturn(Instant.EPOCH));invalid(j->when(j.getExpiresAt()).thenReturn(Instant.parse("2027-01-01T00:00:00Z")));invalid(j->when(j.getIssuedAt()).thenReturn(Instant.parse("2026-01-01T00:01:00Z")));
  invalid(j->when(j.getId()).thenReturn(null));invalid(j->when(j.getId()).thenReturn(" "));invalid(j->when(j.getClaims()).thenReturn(Map.of("roles","CLIENT")));invalid(j->when(j.getClaims()).thenReturn(Map.of("roles",List.of(123))));
 }
}
