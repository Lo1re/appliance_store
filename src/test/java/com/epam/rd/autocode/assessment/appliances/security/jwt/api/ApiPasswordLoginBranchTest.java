package com.epam.rd.autocode.assessment.appliances.security.jwt.api;
import com.epam.rd.autocode.assessment.appliances.security.jwt.TokenPair;
import com.epam.rd.autocode.assessment.appliances.security.jwt.refresh.RefreshTokenService;
import com.epam.rd.autocode.assessment.appliances.security.login.LoginAttemptService;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
class ApiPasswordLoginBranchTest {
    @Test void passwordLoginWithoutPreviousBrowserIssuesNewPair() {
        var authentication=mock(AuthenticationManager.class);var refresh=mock(RefreshTokenService.class);var account=mock(Authentication.class);var pair=mock(TokenPair.class);
        when(authentication.authenticate(any())).thenReturn(account);when(refresh.issue(account,null)).thenReturn(pair);
        assertThat(new ApiPasswordLogin(authentication,refresh,mock(LoginAttemptService.class)).login("user@example.test","password")).isSameAs(pair);
        verify(refresh).issue(account,null);
    }
}
