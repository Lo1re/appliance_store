package com.epam.rd.autocode.assessment.appliances.security.jwt;

import com.epam.rd.autocode.assessment.appliances.security.CurrentActor;
import com.epam.rd.autocode.assessment.appliances.security.MvcSecurityFailures;
import com.epam.rd.autocode.assessment.appliances.security.WebAccessRules;
import com.epam.rd.autocode.assessment.appliances.security.login.PasswordLoginFailureHandler;
import com.epam.rd.autocode.assessment.appliances.web.error.MvcRouteClassifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.session.NullAuthenticatedSessionStrategy;
import org.springframework.security.web.context.NullSecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.savedrequest.NullRequestCache;

@Configuration
@ConditionalOnProperty(name = "store.security.web-auth", havingValue = "jwt", matchIfMissing = true)
public class JwtWebSecurityConfiguration {
    @Bean
    public SecurityFilterChain jwtWebChain(HttpSecurity http, WebAccessRules accessRules, MvcSecurityFailures failures,
            PasswordLoginFailureHandler loginFailures, JwtDecoder decoder, CurrentActor actor,
            RefreshTokenService tokens, AuthTokenCookies accessCookies, JwtCookieLifecycle cookies,
            CookieCsrfTokenRepository csrfTokens, MvcRouteClassifier routes) throws Exception {
        var entryPoint = new JwtBrowserFailures(accessCookies, failures, routes);
        return http
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                        .sessionAuthenticationStrategy(new NullAuthenticatedSessionStrategy()))
                .securityContext(context -> context.securityContextRepository(new NullSecurityContextRepository()))
                .requestCache(cache -> cache.requestCache(new NullRequestCache()))
                .csrf(csrf -> csrf.csrfTokenRepository(csrfTokens)
                        // Cookie lifecycle rotates CSRF on login/refresh/logout, not on every bearer request.
                        .sessionAuthenticationStrategy(new NullAuthenticatedSessionStrategy())
                        .withObjectPostProcessor(new CookieCsrfProtection()))
                .addFilterAfter(new JwtAccountFilter(actor, accessCookies, entryPoint, true), BearerTokenAuthenticationFilter.class)
                .authorizeHttpRequests(accessRules)
                .httpBasic(AbstractHttpConfigurer::disable)
                .exceptionHandling(errors -> errors.authenticationEntryPoint(entryPoint).accessDeniedHandler(failures))
                .formLogin(form -> form.loginPage("/login").loginProcessingUrl("/login")
                        .successHandler(new JwtLoginSuccessHandler(tokens, cookies, accessCookies)).failureHandler(loginFailures).permitAll())
                .logout(logout -> logout.addLogoutHandler(new RefreshLogoutHandler(tokens, accessCookies, cookies))
                        .invalidateHttpSession(false).deleteCookies("JSESSIONID").logoutSuccessHandler(new JwtLogoutSuccessHandler()).permitAll())
                .oauth2ResourceServer(resource -> resource.bearerTokenResolver(new BrowserAccessTokenResolver(accessCookies))
                        .jwt(jwt -> jwt.decoder(decoder).jwtAuthenticationConverter(new AccessTokenAuthenticationConverter()))
                        .authenticationEntryPoint(entryPoint).accessDeniedHandler(failures))
                .build();
    }
}
