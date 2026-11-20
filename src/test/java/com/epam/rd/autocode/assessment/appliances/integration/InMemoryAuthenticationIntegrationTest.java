package com.epam.rd.autocode.assessment.appliances.integration;

import com.epam.rd.autocode.assessment.appliances.security.AccountPrincipal;
import com.epam.rd.autocode.assessment.appliances.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:inmemory-auth;DB_CLOSE_DELAY=-1",
        "store.security.auth-mode=inmemory",
        "store.security.inmemory.accounts[0].email=mercury@gmail.com",
        "store.security.inmemory.accounts[0].password-hash=$2a$10$PsRnmS7huldXCKf0kWLObOB7ndzQZaCC3Bt/gO6X4kohWInxkC//W",
        "store.security.inmemory.accounts[0].role=CLIENT",
        "store.security.inmemory.accounts[1].email=absent@example.com",
        "store.security.inmemory.accounts[1].password-hash=$2a$10$PsRnmS7huldXCKf0kWLObOB7ndzQZaCC3Bt/gO6X4kohWInxkC//W"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class InMemoryAuthenticationIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;

    @Test
    void acceptsOnlyConfiguredMemoryPasswordAndKeepsTheRealLocalIdentity() throws Exception {
        // The test intentionally gives Mercury a different password from the database seed.
        mvc.perform(formLogin().user("mercury@gmail.com").password("MercuryLocal2026!"))
                .andExpect(unauthenticated());
        var login = mvc.perform(formLogin().user(" MERCURY@GMAIL.COM ").password("PhobosLocal2026!"))
                .andExpect(authenticated().withRoles("CLIENT")).andReturn();
        var session = (MockHttpSession) login.getRequest().getSession(false);
        var context = (SecurityContext) session.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
        assertThat(((AccountPrincipal) context.getAuthentication().getPrincipal()).getAccountId())
                .isEqualTo(users.findByEmailIgnoreCase("mercury@gmail.com").orElseThrow().getId());
        mvc.perform(get("/orders").session(session)).andExpect(status().isOk());
        mvc.perform(get("/clients").session(session)).andExpect(status().isForbidden());
        mvc.perform(formLogin().user("absent@example.com").password("PhobosLocal2026!"))
                .andExpect(unauthenticated());
        mvc.perform(formLogin().user("phobos@gmail.com").password("PhobosLocal2026!"))
                .andExpect(unauthenticated());
    }
}
