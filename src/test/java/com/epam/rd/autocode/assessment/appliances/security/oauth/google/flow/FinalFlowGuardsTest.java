package com.epam.rd.autocode.assessment.appliances.security.oauth.google.flow;
import com.epam.rd.autocode.assessment.appliances.security.jwt.cookie.AuthTokenCookies;
import com.epam.rd.autocode.assessment.appliances.security.jwt.access.AccessTokenVersionService;
import com.epam.rd.autocode.assessment.appliances.security.password.reset.ResetTokenCodec;
import com.epam.rd.autocode.assessment.appliances.security.oauth.google.identity.GoogleLoginException;
import java.time.*;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
class FinalFlowGuardsTest {
 @Test void anUnrelatedSessionHasNoLinkIntentAndMissingCookieCannotAuthorizeLinking(){var request=new MockHttpServletRequest();request.getSession();assertThat(new GoogleFlowSession(Clock.systemUTC()).link(request)).isEmpty();var cookies=mock(AuthTokenCookies.class);var decoder=mock(JwtDecoder.class);var verifier=new GoogleLinkVerifier(cookies,decoder,new ResetTokenCodec(),mock(AccessTokenVersionService.class));assertThatThrownBy(()->verifier.verify(request,new GoogleFlowSession.LinkIntent(1L,"hash",Instant.MAX))).isInstanceOf(GoogleLoginException.class);verifyNoInteractions(decoder);}
}
