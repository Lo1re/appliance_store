package com.epam.rd.autocode.assessment.appliances.security.jwt.web;
import com.epam.rd.autocode.assessment.appliances.security.jwt.cookie.AuthTokenCookies;
import jakarta.servlet.DispatcherType;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
class BrowserResolverEdgesTest {
 @Test void staticAndCredentialFlowRequestsNeverReadTheAccessCookie(){var cookies=mock(AuthTokenCookies.class);var resolver=new BrowserAccessTokenResolver(cookies);for(String path:new String[]{"/login","/logout","/auth/renew","/css/style.css","/js/app.js","/images/picture.svg"})assertThat(resolver.resolve(new MockHttpServletRequest("GET",path))).isNull();var error=new MockHttpServletRequest("GET","/private");error.setDispatcherType(DispatcherType.ERROR);assertThat(resolver.resolve(error)).isNull();verifyNoInteractions(cookies);}
 @Test void ambiguousCookiesBecomeSafeProtocolErrors(){var cookies=mock(AuthTokenCookies.class);var request=new MockHttpServletRequest("GET","/orders");when(cookies.read(request)).thenThrow(new IllegalArgumentException("private"));assertThatThrownBy(()->new BrowserAccessTokenResolver(cookies).resolve(request)).isInstanceOf(OAuth2AuthenticationException.class).hasMessage("Invalid browser credentials");}
}
