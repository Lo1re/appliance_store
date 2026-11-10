package com.epam.rd.autocode.assessment.appliances.config;

import com.epam.rd.autocode.assessment.appliances.model.Client;
import com.epam.rd.autocode.assessment.appliances.model.Employee;
import com.epam.rd.autocode.assessment.appliances.model.User;
import com.epam.rd.autocode.assessment.appliances.repository.UserRepository;
import jakarta.servlet.DispatcherType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.sql.init.dependency.DependsOnDatabaseInitialization;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .authorizeHttpRequests(requests -> requests
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers("/", "/css/**", "/js/**", "/images/**").permitAll()
                        .requestMatchers("/employees/**", "/clients/**").hasRole("EMPLOYEE")
                        .requestMatchers(HttpMethod.GET, "/appliances", "/manufacturers")
                        .hasAnyRole("CLIENT", "EMPLOYEE")
                        .requestMatchers("/appliances/**", "/manufacturers/**")
                        .hasRole("EMPLOYEE")
                        .requestMatchers("/orders/**").hasAnyRole("CLIENT", "EMPLOYEE")
                        .anyRequest().denyAll()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .failureUrl("/login?error")
                        .defaultSuccessUrl("/", false)
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutSuccessUrl("/login?logout")
                        .permitAll()
                )
                .build();
    }

    @Bean
    @DependsOnDatabaseInitialization
    public UserDetailsService stageOneUsers(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${store.demo.client-password:ClientDemo2026!}") String clientPassword,
            @Value("${store.demo.employee-password:EmployeeDemo2026!}") String employeePassword
    ) {
        Long clientId = requireSeedAccount(userRepository, "mercury@gmail.com", Client.class);
        Long employeeId = requireSeedAccount(userRepository, "phobos@gmail.com", Employee.class);

        String clientHash = passwordEncoder.encode(clientPassword);
        String employeeHash = passwordEncoder.encode(employeePassword);

        return username -> switch (username) {
            case "client" -> principal(
                    userRepository, clientId, username, clientHash, "CLIENT"
            );
            case "employee" -> principal(
                    userRepository, employeeId, username, employeeHash, "EMPLOYEE"
            );
            default -> throw new UsernameNotFoundException("Account not found");
        };
    }

    private Long requireSeedAccount(
            UserRepository repository,
            String email,
            Class<? extends User> accountType
    ) {
        return repository.findByEmailIgnoreCase(email)
                .filter(accountType::isInstance)
                .map(User::getId)
                .orElseThrow(() -> new IllegalStateException(
                        "Missing demo account in SQL seed data: " + email
                ));
    }

    private StorePrincipal principal(
            UserRepository repository,
            Long accountId,
            String username,
            String passwordHash,
            String role
    ) {
        if (!repository.existsById(accountId)) {
            throw new UsernameNotFoundException("Account not found");
        }

        return new StorePrincipal(accountId, username, passwordHash, role);
    }
}