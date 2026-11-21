package com.epam.rd.autocode.assessment.appliances.security;

import jakarta.servlet.DispatcherType;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.stereotype.Component;

/** One URL authorization policy, independent of how credentials reach the application. */
@Component
public class WebAccessRules implements Customizer<AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry> {
    @Override
    public void customize(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry requests) {
        requests
                .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                .requestMatchers(
                        "/",
                        "/css/**",
                        "/js/**",
                        "/images/**"
                ).permitAll()
                .requestMatchers(HttpMethod.GET, "/login").permitAll()
                .requestMatchers("/lookups/clients").hasRole("EMPLOYEE")
                .requestMatchers(HttpMethod.GET, "/lookups/products", "/lookups/manufacturers")
                .hasAnyRole("CLIENT", "EMPLOYEE")
                .requestMatchers("/employees/**", "/clients/**")
                .hasRole("EMPLOYEE")
                .requestMatchers(
                        HttpMethod.GET,
                        "/appliances",
                        "/manufacturers"
                ).hasAnyRole("CLIENT", "EMPLOYEE")
                .requestMatchers(
                        "/appliances/**",
                        "/manufacturers/**"
                ).hasRole("EMPLOYEE")
                .requestMatchers("/orders/**")
                .hasAnyRole("CLIENT", "EMPLOYEE")
                .anyRequest().denyAll();
    }
}
