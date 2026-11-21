package com.epam.rd.autocode.assessment.appliances.security.jwt;

import com.epam.rd.autocode.assessment.appliances.dto.auth.AccessTokenResponse;
import com.epam.rd.autocode.assessment.appliances.security.login.LoginAttemptService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "store.api.enabled", havingValue = "true")
public class ApiPasswordLogin {
    private final AuthenticationManager authentication;
    private final AccessTokenIssuer tokens;
    private final LoginAttemptService attempts;

    public AccessTokenResponse login(String email, String password) {
        try {
            var account = authentication.authenticate(UsernamePasswordAuthenticationToken.unauthenticated(email, password));
            return tokens.issue(account);
        } catch (LockedException blocked) {
            throw new PasswordLoginLockedException(attempts.remainingLock(email).toSeconds() + 1);
        }
    }
}
