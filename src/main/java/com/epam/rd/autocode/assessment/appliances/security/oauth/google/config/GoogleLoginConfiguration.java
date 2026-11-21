package com.epam.rd.autocode.assessment.appliances.security.oauth.google.config;

import com.epam.rd.autocode.assessment.appliances.security.identity.CurrentActor;
import com.epam.rd.autocode.assessment.appliances.security.jwt.access.AccessTokenAuthenticationConverter;
import com.epam.rd.autocode.assessment.appliances.security.jwt.cookie.AuthTokenCookies;
import com.epam.rd.autocode.assessment.appliances.security.jwt.cookie.CookieCsrfProtection;
import com.epam.rd.autocode.assessment.appliances.security.jwt.cookie.JwtCookieLifecycle;
import com.epam.rd.autocode.assessment.appliances.security.jwt.refresh.RefreshTokenService;
import com.epam.rd.autocode.assessment.appliances.security.jwt.web.BrowserAccessTokenResolver;
import com.epam.rd.autocode.assessment.appliances.security.jwt.web.JwtAccountFilter;
import com.epam.rd.autocode.assessment.appliances.security.oauth.google.flow.DiscardingAuthorizedClientRepository;
import com.epam.rd.autocode.assessment.appliances.security.oauth.google.flow.GoogleAuthorizationRequestRepository;
import com.epam.rd.autocode.assessment.appliances.security.oauth.google.flow.GoogleFlowSession;
import com.epam.rd.autocode.assessment.appliances.security.oauth.google.flow.GoogleLinkVerifier;
import com.epam.rd.autocode.assessment.appliances.security.oauth.google.identity.GoogleAccountAuthentication;
import com.epam.rd.autocode.assessment.appliances.security.oauth.google.identity.GoogleIdentityService;
import com.epam.rd.autocode.assessment.appliances.security.oauth.google.identity.GoogleIdentityVerifier;
import com.epam.rd.autocode.assessment.appliances.security.oauth.google.web.GoogleLoginFailureHandler;
import com.epam.rd.autocode.assessment.appliances.security.oauth.google.web.GoogleLoginSuccessHandler;
import com.epam.rd.autocode.assessment.appliances.security.password.reset.ResetTokenCodec;
import com.epam.rd.autocode.assessment.appliances.security.web.MvcSecurityFailures;
import java.net.URI;
import java.time.Clock;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestCustomizers;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.session.NullAuthenticatedSessionStrategy;
import org.springframework.security.web.context.NullSecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.savedrequest.NullRequestCache;

@Configuration(proxyBeanMethods = false)
@ConditionalOnGoogleLogin
@EnableConfigurationProperties(GoogleLoginProperties.class)
public class GoogleLoginConfiguration {
    @Bean
    public ClientRegistrationRepository googleClientRegistrations(GoogleLoginProperties properties, Environment environment) {
        if (!"jwt".equals(environment.getProperty("store.security.web-auth", "jwt"))
                || !"database".equals(environment.getProperty("store.security.auth-mode", "database")))
            throw new IllegalArgumentException("Google sign-in requires database authentication and JWT web mode");
        URI redirect = URI.create(properties.getRedirectUri());
        boolean localHttp = "http".equals(redirect.getScheme()) && ("localhost".equals(redirect.getHost()) || "127.0.0.1".equals(redirect.getHost()))
                && environment.acceptsProfiles(Profiles.of("local", "test")) && !environment.acceptsProfiles(Profiles.of("prod"));
        if ((!"https".equals(redirect.getScheme()) && !localHttp) || redirect.getHost() == null
                || redirect.getUserInfo() != null || redirect.getQuery() != null || redirect.getFragment() != null
                || !redirect.getPath().endsWith("/login/oauth2/code/google"))
            throw new IllegalArgumentException("Google callback requires a configured trusted HTTPS URI (loopback HTTP allowed only locally)");
        return new InMemoryClientRegistrationRepository(CommonOAuth2Provider.GOOGLE.getBuilder("google")
                .clientId(properties.getClientId()).clientSecret(properties.getClientSecret())
                .scope("openid", "profile", "email").redirectUri(properties.getRedirectUri()).build());
    }

    @Bean @Order(0)
    public SecurityFilterChain googleFlowChain(HttpSecurity http, ClientRegistrationRepository registrations,
            Clock clock, CurrentActor actor, MvcSecurityFailures failures, JwtDecoder decoder,
            AuthTokenCookies browserCookies, JwtCookieLifecycle cookies, RefreshTokenService tokens,
            CookieCsrfTokenRepository csrfTokens, GoogleIdentityService identities, GoogleIdentityVerifier verifier,
            GoogleAccountAuthentication accounts, GoogleFlowSession flows, ResetTokenCodec hashes) throws Exception {
        var resolver = new DefaultOAuth2AuthorizationRequestResolver(registrations, "/oauth2/authorization");
        resolver.setAuthorizationRequestCustomizer(OAuth2AuthorizationRequestCustomizers.withPkce());
        var success = new GoogleLoginSuccessHandler(verifier, identities, accounts, flows, tokens, cookies, browserCookies,
                new GoogleLinkVerifier(browserCookies, decoder, hashes));
        var bearer = new BrowserAccessTokenResolver(browserCookies);
        return http.securityMatcher("/oauth2/**", "/login/oauth2/**", "/onboarding/google/**", "/account/google/**")
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                        .sessionAuthenticationStrategy(new NullAuthenticatedSessionStrategy()))
                .securityContext(context -> context.securityContextRepository(new NullSecurityContextRepository()))
                .requestCache(cache -> cache.requestCache(new NullRequestCache()))
                .csrf(csrf -> csrf.csrfTokenRepository(csrfTokens).sessionAuthenticationStrategy(new NullAuthenticatedSessionStrategy())
                        .withObjectPostProcessor(new CookieCsrfProtection()))
                .authorizeHttpRequests(access -> access
                        .requestMatchers("/account/google/**").hasAnyRole("CLIENT", "EMPLOYEE")
                        .requestMatchers(HttpMethod.GET, "/oauth2/authorization/google", "/login/oauth2/code/google", "/onboarding/google", "/onboarding/google/verify").permitAll()
                        .requestMatchers(HttpMethod.POST, "/onboarding/google", "/onboarding/google/cancel").permitAll()
                        .anyRequest().denyAll())
                .addFilterAfter(new JwtAccountFilter(actor, browserCookies, failures, true), BearerTokenAuthenticationFilter.class)
                .exceptionHandling(errors -> errors.authenticationEntryPoint(failures).accessDeniedHandler(failures))
                .formLogin(AbstractHttpConfigurer::disable).httpBasic(AbstractHttpConfigurer::disable).logout(AbstractHttpConfigurer::disable)
                .oauth2Login(login -> login.clientRegistrationRepository(registrations).loginPage("/login")
                        .authorizedClientRepository(new DiscardingAuthorizedClientRepository())
                        .authorizationEndpoint(endpoint -> endpoint.authorizationRequestResolver(resolver)
                                .authorizationRequestRepository(new GoogleAuthorizationRequestRepository(clock)))
                        .successHandler(success).failureHandler(new GoogleLoginFailureHandler(flows)))
                .oauth2ResourceServer(resource -> resource.bearerTokenResolver(request ->
                        request.getRequestURI().substring(request.getContextPath().length()).startsWith("/account/google") ? bearer.resolve(request) : null)
                        .jwt(jwt -> jwt.decoder(decoder).jwtAuthenticationConverter(new AccessTokenAuthenticationConverter()))
                        .authenticationEntryPoint(failures).accessDeniedHandler(failures))
                .build();
    }
}
