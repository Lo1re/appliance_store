package com.epam.rd.autocode.assessment.appliances.security.config;

import com.epam.rd.autocode.assessment.appliances.security.authentication.AuthenticationProperties;
import com.epam.rd.autocode.assessment.appliances.security.authorization.WebAccessRules;
import com.epam.rd.autocode.assessment.appliances.security.identity.ActiveAccountFilter;
import com.epam.rd.autocode.assessment.appliances.security.identity.CurrentActor;
import com.epam.rd.autocode.assessment.appliances.security.login.LoginAttemptService;
import com.epam.rd.autocode.assessment.appliances.security.login.LoginProtectedAuthenticationProvider;
import com.epam.rd.autocode.assessment.appliances.security.login.LoginProtectionProperties;
import com.epam.rd.autocode.assessment.appliances.security.login.PasswordLoginFailureHandler;
import com.epam.rd.autocode.assessment.appliances.security.session.SessionPolicyProperties;
import com.epam.rd.autocode.assessment.appliances.security.web.MvcSecurityFailures;
import java.time.Clock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.web.session.HttpSessionEventPublisher;

@Configuration
@EnableMethodSecurity
@EnableConfigurationProperties({AuthenticationProperties.class, LoginProtectionProperties.class, SessionPolicyProperties.class, WebAuthenticationProperties.class})
@EnableScheduling
public class SecurityConfig {

    @Bean
    public Clock securityClock() { return Clock.systemUTC(); }

    @Bean
    public SessionRegistry sessionRegistry() { return new SessionRegistryImpl(); }

    @Bean
    public HttpSessionEventPublisher sessionEventPublisher() { return new HttpSessionEventPublisher(); }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationProvider authenticationProvider(
            UserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder, LoginAttemptService attempts
    ) {
        var provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return new LoginProtectedAuthenticationProvider(provider, attempts);
    }

    @Bean
    @ConditionalOnProperty(name = "store.security.web-auth", havingValue = "session")
    public SecurityFilterChain securityFilterChain(HttpSecurity http, MvcSecurityFailures failures,
                                                    CurrentActor actor, SessionRegistry sessions,
                                                    SessionPolicyProperties sessionPolicy, PasswordLoginFailureHandler loginFailures, WebAccessRules accessRules, WebSecurityHeaders headers)
            throws Exception {
        return http.headers(headers)
                .addFilterAfter(new ActiveAccountFilter(actor), AnonymousAuthenticationFilter.class)
                .sessionManagement(session -> {
                    session.sessionFixation(fixation -> fixation.changeSessionId());
                    session.maximumSessions(sessionPolicy.maximum()).maxSessionsPreventsLogin(false)
                            .sessionRegistry(sessions).expiredUrl("/login?expired");
                })
                .authorizeHttpRequests(accessRules)
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint(failures)
                        .accessDeniedHandler(failures)
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .failureHandler(loginFailures)
                        .defaultSuccessUrl("/", false)
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutSuccessUrl("/login?logout")
                        .permitAll()
                )
                .build();
    }
}
