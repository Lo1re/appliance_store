package com.epam.rd.autocode.assessment.appliances.controller.auth;
import com.epam.rd.autocode.assessment.appliances.security.jwt.cookie.*;
import com.epam.rd.autocode.assessment.appliances.security.jwt.refresh.*;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
class JwtRenewalControllerBranchTest {
    @Test void malformedRefreshCookieClearsCredentialsAndRedirectsToLogin() {
        var refresh=mock(RefreshTokenService.class);var browser=mock(AuthTokenCookies.class);var cookies=mock(JwtCookieLifecycle.class);var request=new MockHttpServletRequest();var response=new MockHttpServletResponse();
        when(browser.readRefresh(request)).thenThrow(new IllegalArgumentException("private"));
        assertThat(new JwtRenewalController(refresh,browser,cookies).renew(request,response,new RedirectAttributesModelMap())).isEqualTo("redirect:/login?expired");verify(cookies).clear(request,response);verifyNoInteractions(refresh);
    }
}
