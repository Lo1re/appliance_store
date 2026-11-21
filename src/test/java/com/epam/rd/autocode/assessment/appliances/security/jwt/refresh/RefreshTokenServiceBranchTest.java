package com.epam.rd.autocode.assessment.appliances.security.jwt.refresh;
import com.epam.rd.autocode.assessment.appliances.dto.auth.AccessTokenResponse;
import com.epam.rd.autocode.assessment.appliances.model.*;
import com.epam.rd.autocode.assessment.appliances.repository.*;
import com.epam.rd.autocode.assessment.appliances.security.identity.AccountPrincipal;
import com.epam.rd.autocode.assessment.appliances.security.jwt.access.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class RefreshTokenServiceBranchTest {
    final RefreshTokenRepository tokens=mock(RefreshTokenRepository.class);final UserRepository users=mock(UserRepository.class);
    final RefreshTokenCodec codec=new RefreshTokenCodec();final AccessTokenIssuer access=mock(AccessTokenIssuer.class);
    final Instant now=Instant.parse("2026-11-22T00:00:00Z");
    final RefreshTokenService service=new RefreshTokenService(tokens,users,codec,new RefreshTokenProperties(Duration.ofDays(7),Duration.ofDays(8),3600000),access,Clock.fixed(now,ZoneOffset.UTC),manager());
    final String raw=codec.generate();final String hash=codec.hash(raw);final Client client=new Client(1L,"Client","client@example.test","hash",null);
    PlatformTransactionManager manager(){var manager=mock(PlatformTransactionManager.class);when(manager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());return manager;}
    Authentication auth(String... roles){return UsernamePasswordAuthenticationToken.authenticated(new AccessTokenPrincipal(1L),null,AuthorityUtils.createAuthorityList(roles));}
    void account(){when(users.lockIdentity(1L)).thenReturn(Optional.of(1L));when(users.findById(1L)).thenReturn(Optional.of(client));}
    void owner(){when(tokens.findOwnerByHash(hash)).thenReturn(Optional.of(1L));when(users.lockIdentity(1L)).thenReturn(Optional.of(1L));}
    @Test void rejectsEveryUnsupportedAuthenticationShapeBeforeStorage() {
        var principal=mock(AccountPrincipal.class);when(principal.getAccountId()).thenReturn(null);
        for(Authentication authentication:Arrays.asList(null,UsernamePasswordAuthenticationToken.unauthenticated("user","pw"),UsernamePasswordAuthenticationToken.authenticated("user",null,List.of()),UsernamePasswordAuthenticationToken.authenticated(principal,null,List.of())))
            assertThatThrownBy(()->service.issue(authentication)).isInstanceOf(InvalidRefreshTokenException.class);
        verifyNoInteractions(users,tokens);
    }
    @Test void declaredAuthoritiesMustContainExactlyOneMatchingDomainRole() {
        account();for(String[] authorities:new String[][]{ {}, {"ROLE_ADMIN"}, {"ROLE_CLIENT","ROLE_EMPLOYEE"}, {"ROLE_EMPLOYEE"} })
            assertThatThrownBy(()->service.issue(auth(authorities))).isInstanceOf(InvalidRefreshTokenException.class);
    }
    @Test void missingCurrentAccountCannotReceiveCredentials() {
        assertThatThrownBy(()->service.issue(auth("ROLE_CLIENT"))).isInstanceOf(InvalidRefreshTokenException.class);
        when(users.lockIdentity(1L)).thenReturn(Optional.of(1L));
        assertThatThrownBy(()->service.issue(auth("ROLE_CLIENT"))).isInstanceOf(InvalidRefreshTokenException.class);
    }
    @Test void switchingFromDeletedPreviousOwnerStillAllowsCurrentLogin() {
        account();when(tokens.findOwnerByHash(hash)).thenReturn(Optional.of(2L));
        var saved=mock(RefreshToken.class);when(saved.getId()).thenReturn(10L);when(tokens.saveAndFlush(any())).thenReturn(saved);
        when(access.issue(any())).thenReturn(new AccessTokenResponse("access","Bearer",900,now.plusSeconds(900)));
        assertThat(service.issue(auth("ROLE_CLIENT"),raw).access().accessToken()).isEqualTo("access");verify(users).lockIdentity(2L);
    }
    @Test void malformedRefreshIsRejectedWithoutDatabaseAccess() {assertThatThrownBy(()->service.rotate("invalid")).isInstanceOf(InvalidRefreshTokenException.class);verifyNoInteractions(tokens,users);}
    @Test void missingOwnerOrDeletedOwnerCannotRefreshOrRevoke() {
        assertThatThrownBy(()->service.rotate(raw)).isInstanceOf(InvalidRefreshTokenException.class);service.revoke(raw);
        when(tokens.findOwnerByHash(hash)).thenReturn(Optional.of(1L));
        assertThatThrownBy(()->service.rotate(raw)).isInstanceOf(InvalidRefreshTokenException.class);service.revoke(raw);
        verify(tokens,never()).revokeAll(any(),any());
    }
    @Test void tokenRemovedAfterOwnerLookupIsRejected() {owner();assertThatThrownBy(()->service.rotate(raw)).isInstanceOf(InvalidRefreshTokenException.class);service.revoke(raw);}
    @Test void deletedOrUnsupportedDomainProfileCannotRefresh() {
        owner();when(tokens.findByTokenHash(hash)).thenReturn(Optional.of(new RefreshToken(1L,hash,now.plusSeconds(60))));
        assertThatThrownBy(()->service.rotate(raw)).isInstanceOf(InvalidRefreshTokenException.class);
        when(users.findById(1L)).thenReturn(Optional.of(new User()));
        assertThatThrownBy(()->service.rotate(raw)).isInstanceOf(InvalidRefreshTokenException.class);
    }
    @Test void corruptedSuccessorChainCannotCrossOwnersOrLoopForever() {
        owner();var first=mock(RefreshToken.class);var next=mock(RefreshToken.class);
        when(first.getId()).thenReturn(1L);when(first.getUserId()).thenReturn(1L);when(first.getReplacedBy()).thenReturn(2L);
        when(tokens.findByTokenHash(hash)).thenReturn(Optional.of(first));when(tokens.findById(2L)).thenReturn(Optional.of(next));
        when(next.getUserId()).thenReturn(2L);service.revoke(raw);verify(next,never()).revoke(any());
        when(next.getUserId()).thenReturn(1L);when(next.getId()).thenReturn(1L);service.revoke(raw);verify(next,never()).revoke(any());
    }
    @Test void cleanupSkipsOwnerDeletedBeforeLock() {
        when(tokens.findExpiredOwners(any(),any())).thenReturn(List.of(1L));assertThat(service.cleanup()).isZero();verify(tokens,never()).deleteExpired(any(),any());
    }
    @Test void storageFailureDoesNotExposeDatabaseDetails() {
        when(tokens.findOwnerByHash(hash)).thenThrow(new DataAccessResourceFailureException("private"));assertThatThrownBy(()->service.rotate(raw)).isInstanceOf(AuthenticationServiceException.class).hasMessageNotContaining("private");
    }
}
