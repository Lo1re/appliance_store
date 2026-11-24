package com.epam.rd.autocode.assessment.appliances.security.identity;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.epam.rd.autocode.assessment.appliances.security.jwt.access.*;
import com.epam.rd.autocode.assessment.appliances.security.logging.SecurityLogContext;

import jakarta.servlet.FilterChain;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.util.List;

class FinalIdentityGuardsTest {
    @Test
    void selfEqualityAndTokenExpiryHaveStableContracts() {
        var principal = new StorePrincipal(1L, "name", "hash", AccountRole.CLIENT);
        assertThat(principal.equals(principal)).isTrue();
        assertThat(
                        new AccessTokenAuthentication(
                                        new AccessTokenPrincipal(1L), List.of(), Instant.EPOCH)
                                .getExpiresAt())
                .isEqualTo(Instant.EPOCH);
        assertThat(SecurityLogContext.remoteAddress(null)).isEqualTo("-");
    }

    @Test
    void unrelatedPrincipalsDoNotTriggerAccountLookups() throws Exception {
        var actor = mock(CurrentActor.class);
        var chain = mock(FilterChain.class);
        SecurityContextHolder.getContext()
                .setAuthentication(
                        UsernamePasswordAuthenticationToken.unauthenticated("name", "secret"));
        try {
            var request = new MockHttpServletRequest();
            var response = new MockHttpServletResponse();
            new ActiveAccountFilter(actor).doFilter(request, response, chain);
            verifyNoInteractions(actor);
            verify(chain).doFilter(request, response);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    void absentAuthenticationPassesThroughWithoutLookup() throws Exception {
        SecurityContextHolder.clearContext();
        var actor = mock(CurrentActor.class);
        var chain = mock(FilterChain.class);
        var request = new MockHttpServletRequest();
        var response = new MockHttpServletResponse();
        new ActiveAccountFilter(actor).doFilter(request, response, chain);
        verifyNoInteractions(actor);
        verify(chain).doFilter(request, response);
    }
}
