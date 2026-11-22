package com.epam.rd.autocode.assessment.appliances.security.jwt.api;

import com.epam.rd.autocode.assessment.appliances.api.error.ApiSecurityFailures;
import com.epam.rd.autocode.assessment.appliances.security.config.WebSecurityHeaders;
import com.epam.rd.autocode.assessment.appliances.security.identity.CurrentActor;
import com.epam.rd.autocode.assessment.appliances.security.jwt.access.AccessTokenAuthenticationConverter;
import com.epam.rd.autocode.assessment.appliances.security.jwt.cookie.AuthTokenCookies;
import com.epam.rd.autocode.assessment.appliances.security.jwt.cookie.CookieCsrfProtection;
import com.epam.rd.autocode.assessment.appliances.security.jwt.web.JwtAccountFilter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
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
@ConditionalOnProperty(name = "store.api.enabled", havingValue = "true")
public class ApiSecurityConfiguration {
    @Bean @Order(1)
    public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http, JwtDecoder decoder, ApiSecurityFailures failures,
            ApiTransportProperties transport, ApiBearerTokenResolver tokens, CookieCsrfTokenRepository csrfTokens,
            CurrentActor actor, AuthTokenCookies cookies, WebSecurityHeaders headers) throws Exception {
        return http.headers(headers).securityMatcher("/api/**")
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                        .sessionAuthenticationStrategy(new NullAuthenticatedSessionStrategy()))
                .securityContext(context -> context.securityContextRepository(new NullSecurityContextRepository()))
                .requestCache(cache -> cache.requestCache(new NullRequestCache()))
                .addFilterAfter(new JwtAccountFilter(actor, cookies, failures, transport.cookieMode()), BearerTokenAuthenticationFilter.class)
                .csrf(csrf -> {
                    if (transport.cookieMode()) csrf.csrfTokenRepository(csrfTokens)
                        // Cookie lifecycle rotates CSRF on login/refresh/logout, not on every bearer request.
                        .sessionAuthenticationStrategy(new NullAuthenticatedSessionStrategy())
                        .withObjectPostProcessor(new CookieCsrfProtection());
                    else csrf.disable();
                }).formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable).logout(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth.requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/auth/refresh", "/api/auth/logout").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/auth/csrf").permitAll()
                        .anyRequest().hasAnyRole("CLIENT", "EMPLOYEE"))
                .exceptionHandling(errors -> errors.authenticationEntryPoint(failures).accessDeniedHandler(failures))
                .oauth2ResourceServer(resource -> resource.bearerTokenResolver(tokens).jwt(jwt -> jwt.decoder(decoder)
                        .jwtAuthenticationConverter(new AccessTokenAuthenticationConverter()))
                        .authenticationEntryPoint(failures).accessDeniedHandler(failures))
                .build();
    }
}
