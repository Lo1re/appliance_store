package com.epam.rd.autocode.assessment.appliances.security.jwt.access;
import com.epam.rd.autocode.assessment.appliances.security.jwt.config.JwtProperties;
import java.time.*;import java.util.*;import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
class CanonicalSubjectEquivalenceTest {
 final Instant now=Instant.parse("2026-01-01T00:00:00Z");
 Jwt jwt(AtomicReference<String> subject){var jwt=mock(Jwt.class);when(jwt.getSubject()).thenAnswer(call->subject.get());when(jwt.getClaims()).thenReturn(Map.of("roles",List.of("CLIENT")));when(jwt.getAudience()).thenReturn(List.of("appliance-store-api"));when(jwt.getIssuedAt()).thenReturn(now);when(jwt.getExpiresAt()).thenReturn(now.plusSeconds(100));when(jwt.getId()).thenReturn("id");return jwt;}
 boolean previousRule(String value){try{return value!=null&&value.matches("[1-9][0-9]{0,18}")&&Long.parseLong(value)>0;}catch(RuntimeException invalid){return false;}}
 @Test void canonicalNumericCheckPreservesThePreviousAcceptedLanguage(){var subject=new AtomicReference<String>();var jwt=jwt(subject);var validator=new AccessTokenClaimsValidator(new JwtProperties(),Clock.fixed(now,ZoneOffset.UTC));var inputs=new ArrayList<String>(Arrays.asList(null,"","0","-1","+1","01","1","7","9223372036854775807","9223372036854775808","99999999999999999999"," 1","1 ","١","１２"));var random=new Random(17);String alphabet="0123456789+- abc١";for(int i=0;i<2000;i++){var s=new StringBuilder();for(int n=random.nextInt(23);n>0;n--)s.append(alphabet.charAt(random.nextInt(alphabet.length())));inputs.add(s.toString());}for(String input:inputs){subject.set(input);assertThat(!validator.validate(jwt).hasErrors()).as("subject %s",input).isEqualTo(previousRule(input));}}
 @Test void aNegativeCredentialVersionIsRejectedEvenIfTheReaderIsFaulty(){var subject=new AtomicReference<>("1");var jwt=jwt(subject);try(var versions=mockStatic(AccessTokenVersion.class)){versions.when(()->AccessTokenVersion.read(jwt)).thenReturn(-1L);assertThat(new AccessTokenClaimsValidator(new JwtProperties(),Clock.fixed(now,ZoneOffset.UTC)).validate(jwt).hasErrors()).isTrue();}}
}
