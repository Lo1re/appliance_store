package com.epam.rd.autocode.assessment.appliances.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.core.userdetails.User;
import static org.assertj.core.api.Assertions.*;

class StorePrincipalTest {
    @Test
    void sameAccountKeepsIdentityAcrossEmailAndCredentialChanges() {
        var before = principal(7L, "before@example.com");
        var after = principal(7L, "after@example.com");
        assertThat(before).isEqualTo(after).hasSameHashCodeAs(after);
        assertThat(after).isEqualTo(before);
        before.eraseCredentials();
        assertThat(before.getPassword()).isNull();
        assertThat(before).isEqualTo(after);
        assertThat(before.getAuthorities()).extracting(a -> a.getAuthority()).containsExactly("ROLE_EMPLOYEE");
    }

    @Test
    void reusedEmailDoesNotMergeDifferentAccountsOrFrameworkUsers() {
        var first = principal(7L, "same@example.com");
        var second = principal(8L, "same@example.com");
        var frameworkUser = User.withUsername("same@example.com").password("hash").roles("EMPLOYEE").build();
        assertThat(first).isNotEqualTo(second).isNotEqualTo(frameworkUser);
        assertThat(frameworkUser).isNotEqualTo(first);
        assertThat(principal(null, "same@example.com")).isNotEqualTo(principal(null, "same@example.com"));
    }

    @Test
    void registryGroupsTheSameAccountAfterAnEmailChange() {
        var registry = new SessionRegistryImpl();
        var before = principal(7L, "before@example.com");
        var after = principal(7L, "after@example.com");
        registry.registerNewSession("first", before);
        registry.registerNewSession("second", after);
        assertThat(registry.getAllPrincipals()).hasSize(1);
        assertThat(registry.getAllSessions(after, false)).hasSize(2);
    }

    private StorePrincipal principal(Long id, String email) {
        return new StorePrincipal(id, email, "hash", AccountRole.EMPLOYEE);
    }
}
