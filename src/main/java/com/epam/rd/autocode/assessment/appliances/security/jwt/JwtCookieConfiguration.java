package com.epam.rd.autocode.assessment.appliances.security.jwt;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

@Configuration
@ConditionalOnJwt
public class JwtCookieConfiguration {
    @Bean
    public CookieCsrfTokenRepository jwtCsrfTokens(AuthCookieProperties properties, AccessTokenCookies cookies) {
        var repository = new CookieCsrfTokenRepository();
        // A distinct name avoids old /api-scoped CSRF cookies from the previous implementation.
        repository.setCookieName("STORE-CSRF");
        repository.setCookieCustomizer(cookie -> {
            cookie.path(cookies.path()).httpOnly(true).sameSite("Lax");
            if (properties.secure()) cookie.secure(true);
        });
        return repository;
    }
}
