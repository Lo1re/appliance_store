package com.epam.rd.autocode.assessment.appliances.config;

import com.epam.rd.autocode.assessment.appliances.model.Client;
import com.epam.rd.autocode.assessment.appliances.model.Employee;
import com.epam.rd.autocode.assessment.appliances.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class SecurityConfigTest {
    private final UserRepository users = mock(UserRepository.class);
    private final SecurityConfig configuration = new SecurityConfig();
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(4);

    @Test
    void missingOrWrongSeedTypeFailsAtStartup() {
        when(users.findByEmailIgnoreCase("mercury@gmail.com")).thenReturn(Optional.of(new Employee()));

        assertThatThrownBy(() -> configuration.stageOneUsers(users, encoder, "test", "test"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("mercury@gmail.com");
    }

    @Test
    void deletedSeedProfileCannotLogInAgain() {
        Client client = new Client();
        client.setId(1L);
        Employee employee = new Employee();
        employee.setId(2L);
        when(users.findByEmailIgnoreCase("mercury@gmail.com")).thenReturn(Optional.of(client));
        when(users.findByEmailIgnoreCase("phobos@gmail.com")).thenReturn(Optional.of(employee));

        var details = configuration.stageOneUsers(users, encoder, "test", "test");

        assertThatThrownBy(() -> details.loadUserByUsername("client"))
                .isInstanceOf(UsernameNotFoundException.class);
    }
}
