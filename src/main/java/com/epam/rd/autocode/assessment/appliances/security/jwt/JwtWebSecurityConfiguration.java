package com.epam.rd.autocode.assessment.appliances.security.jwt;

import com.epam.rd.autocode.assessment.appliances.security.CurrentActor;
import com.epam.rd.autocode.assessment.appliances.security.MvcSecurityFailures;
import com.epam.rd.autocode.assessment.appliances.security.WebAccessRules;
import com.epam.rd.autocode.assessment.appliances.security.login.PasswordLoginFailureHandler;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.NullSecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.savedrequest.NullRequestCache;

@Configuration
@ConditionalOnProperty(name = "store.security.web-auth", havingValue = "jwt", matchIfMissing = true)
public class JwtWebSecurityConfiguration {
    @Bean
    public SecurityFilterChain jwtWebChain(HttpSecurity http, WebAccessRules accessRules, MvcSecurityFailures failures,
            PasswordLoginFailureHandler loginFailures, JwtDecoder decoder, CurrentActor actor,
            AccessTokenIssuer tokens, AccessTokenCookies accessCookies, JwtCookieLifecycle cookies,
            CookieCsrfTokenRepository csrfTokens) throws Exception {
        var entryPoint = new JwtBrowserFailures(accessCookies, failures);
        return http
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .securityContext(context -> context.securityContextRepository(new NullSecurityContextRepository()))
                .requestCache(cache -> cache.requestCache(new NullRequestCache()))
                .csrf(csrf -> csrf.csrfTokenRepository(csrfTokens).withObjectPostProcessor(new CookieCsrfProtection()))
                .addFilterAfter(new JwtAccountFilter(actor, accessCookies, entryPoint), BearerTokenAuthenticationFilter.class)
                .authorizeHttpRequests(accessRules)
                .httpBasic(AbstractHttpConfigurer::disable)
                .exceptionHandling(errors -> errors.authenticationEntryPoint(entryPoint).accessDeniedHandler(failures))
                .formLogin(form -> form.loginPage("/login").loginProcessingUrl("/login")
                        .successHandler(new JwtLoginSuccessHandler(tokens, cookies)).failureHandler(loginFailures).permitAll())
                .logout(logout -> logout.addLogoutHandler((request, response, authentication) -> cookies.clear(request, response))
                        .invalidateHttpSession(false).deleteCookies("JSESSIONID").logoutSuccessUrl("/login?logout").permitAll())
                .oauth2ResourceServer(resource -> resource.bearerTokenResolver(new BrowserAccessTokenResolver(accessCookies))
                        .jwt(jwt -> jwt.decoder(decoder).jwtAuthenticationConverter(new AccessTokenAuthenticationConverter()))
                        .authenticationEntryPoint(entryPoint).accessDeniedHandler(failures))
                .build();
    }
}
