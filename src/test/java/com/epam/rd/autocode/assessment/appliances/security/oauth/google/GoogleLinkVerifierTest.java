package com.epam.rd.autocode.assessment.appliances.security.oauth.google;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.epam.rd.autocode.assessment.appliances.security.jwt.cookie.AuthTokenCookies;
import com.epam.rd.autocode.assessment.appliances.security.oauth.google.flow.GoogleFlowSession;
import com.epam.rd.autocode.assessment.appliances.security.oauth.google.flow.GoogleLinkVerifier;
import com.epam.rd.autocode.assessment.appliances.security.oauth.google.identity.GoogleLoginException;
import com.epam.rd.autocode.assessment.appliances.security.password.reset.ResetTokenCodec;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import java.time.Instant;

class GoogleLinkVerifierTest {
    @Test
    void rejectsAccountSwitchingAndRequiresTheSameValidJwt() {
        var cookies = mock(AuthTokenCookies.class);
        var decoder = mock(JwtDecoder.class);
        var hashes = new ResetTokenCodec();
        var verifier =
                new GoogleLinkVerifier(
                        cookies,
                        decoder,
                        hashes,
                        mock(
                                com.epam.rd.autocode.assessment.appliances.security.jwt.access
                                        .AccessTokenVersionService.class));
        var request = new MockHttpServletRequest();
        var intent =
                new GoogleFlowSession.LinkIntent(
                        1L, hashes.hash("original-access"), Instant.now().plusSeconds(900));
        when(cookies.read(request)).thenReturn("different-access");
        assertThatThrownBy(() -> verifier.verify(request, intent))
                .isInstanceOf(GoogleLoginException.class);
        verifyNoInteractions(decoder);
        when(cookies.read(request)).thenReturn("original-access");
        when(decoder.decode("original-access"))
                .thenReturn(
                        Jwt.withTokenValue("original-access")
                                .header("alg", "HS256")
                                .subject("2")
                                .build());
        assertThatThrownBy(() -> verifier.verify(request, intent))
                .isInstanceOf(GoogleLoginException.class);
        when(decoder.decode("original-access"))
                .thenReturn(
                        Jwt.withTokenValue("original-access")
                                .header("alg", "HS256")
                                .subject("1")
                                .build());
        assertThatCode(() -> verifier.verify(request, intent)).doesNotThrowAnyException();
    }
}
