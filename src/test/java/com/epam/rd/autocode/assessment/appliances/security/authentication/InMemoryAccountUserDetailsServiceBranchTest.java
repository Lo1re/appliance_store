package com.epam.rd.autocode.assessment.appliances.security.authentication;
import com.epam.rd.autocode.assessment.appliances.model.Client;
import com.epam.rd.autocode.assessment.appliances.repository.UserRepository;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
class InMemoryAccountUserDetailsServiceBranchTest {
    @Test void absentConfiguredRoleUsesCurrentDomainRole() {var users=mock(UserRepository.class);when(users.findByEmailIgnoreCase("user@example.test")).thenReturn(Optional.of(new Client(1L,"Client","user@example.test","db-hash",null)));var credentials=new InMemoryUserDetailsManager(User.withUsername("user@example.test").password("memory-hash").roles("CLIENT").build());var details=new InMemoryAccountUserDetailsService(credentials,users,Map.of()).loadUserByUsername("user@example.test");assertThat(details.getPassword()).isEqualTo("memory-hash");}
    @Test void missingOrBlankUsernameNeverReachesCredentials() {
        var users=mock(UserRepository.class);var credentials=mock(InMemoryUserDetailsManager.class);var service=new InMemoryAccountUserDetailsService(credentials,users,Map.of());
        for(String username:new String[]{null," "}) assertThatThrownBy(()->service.loadUserByUsername(username)).isInstanceOf(org.springframework.security.core.userdetails.UsernameNotFoundException.class);
        verifyNoInteractions(users,credentials);
    }

}
