package com.epam.rd.autocode.assessment.appliances.integration;

import com.epam.rd.autocode.assessment.appliances.model.Employee;
import com.epam.rd.autocode.assessment.appliances.model.Manufacturer;
import com.epam.rd.autocode.assessment.appliances.repository.EmployeeRepository;
import com.epam.rd.autocode.assessment.appliances.repository.ManufacturerRepository;
import com.epam.rd.autocode.assessment.appliances.security.StorePrincipal;
import com.epam.rd.autocode.assessment.appliances.service.ClientService;
import com.epam.rd.autocode.assessment.appliances.service.EmployeeService;
import com.epam.rd.autocode.assessment.appliances.service.LookupService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:active-account-lookups;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ActiveAccountAndLookupIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired EmployeeRepository employees;
    @Autowired EmployeeService employeeService;
    @Autowired ClientService clients;
    @Autowired org.springframework.security.crypto.password.PasswordEncoder encoder;
    @Autowired LookupService lookups;
    @Autowired ManufacturerRepository manufacturers;
    @Autowired SessionRegistry sessions;
    @Autowired PlatformTransactionManager transactions;

    @Test
    void deletedPrincipalCannotCallAnyManagementServiceOrUseAnOldSession() throws Exception {
        var stale = new StorePrincipal(Long.MAX_VALUE, "deleted@example.com", "hash", "EMPLOYEE");
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(stale, "", stale.getAuthorities()));
        SecurityContextHolder.setContext(context);
        try {
            assertThatThrownBy(() -> clients.findAll("", org.springframework.data.domain.PageRequest.of(0, 10)))
                    .isInstanceOf(AccessDeniedException.class);
            var request = new com.epam.rd.autocode.assessment.appliances.dto.client.ClientRequest();
            request.setName("Unauthorized write"); request.setEmail("stale-write@example.com"); request.setCard("TEST");
            var password = new com.epam.rd.autocode.assessment.appliances.dto.user.PasswordRequest();
            password.setPassword("StalePrincipal2026!");
            assertThatThrownBy(() -> clients.create(request, password)).isInstanceOf(AccessDeniedException.class);
            var session = new MockHttpSession();
            session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
            mvc.perform(get("/clients").session(session)).andExpect(status().is3xxRedirection());
            assertThat(session.isInvalid()).isTrue();
        } finally { SecurityContextHolder.clearContext(); }
    }

    @Test
    @WithUserDetails("phobos@gmail.com")
    void sessionRevocationFollowsRealTransactionCommitAndRollback() {
        var target = employees.saveAndFlush(new Employee(null, "Revocation", "revocation@example.com", "hash", "Sales"));
        var principal = new StorePrincipal(target.getId(), target.getEmail(), "hash", "EMPLOYEE");
        sessions.registerNewSession("revocation-target", principal);
        var tx = new TransactionTemplate(transactions);
        tx.executeWithoutResult(status -> {
            employeeService.delete(target.getId());
            assertThat(sessions.getSessionInformation("revocation-target").isExpired()).isFalse();
            status.setRollbackOnly();
        });
        assertThat(employees.existsById(target.getId())).isTrue();
        assertThat(sessions.getSessionInformation("revocation-target").isExpired()).isFalse();
        employeeService.delete(target.getId());
        assertThat(sessions.getSessionInformation("revocation-target").isExpired()).isTrue();
        assertThat(employees.existsById(target.getId())).isFalse();
    }

    @Test
    @WithUserDetails("phobos@gmail.com")
    void realFormLoginIsRegisteredAndDeletedAccountLosesItsSession() throws Exception {
        var admin = SecurityContextHolder.getContext();
        var target = employees.saveAndFlush(new Employee(null, "Session test", "session-test@example.com",
                encoder.encode("SessionTarget2026!"), "Sales"));
        var login = mvc.perform(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders
                        .formLogin().user(target.getEmail()).password("SessionTarget2026!"))
                .andExpect(status().is3xxRedirection()).andReturn();
        var session = (MockHttpSession) login.getRequest().getSession(false);
        assertThat(session).isNotNull();
        assertThat(sessions.getSessionInformation(session.getId())).isNotNull();
        SecurityContextHolder.setContext(admin);
        employeeService.delete(target.getId());
        assertThat(sessions.getSessionInformation(session.getId()).isExpired()).isTrue();
        mvc.perform(get("/clients").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?expired"));
    }

    @Test
    @WithUserDetails("phobos@gmail.com")
    void optionsAreBoundedSearchableAndKeepSelectedItemsOutsideFirstPage() throws Exception {
        var tx = new TransactionTemplate(transactions);
        tx.executeWithoutResult(status -> {
            for (int i = 0; i < 25; i++) {
                var item = new Manufacturer(); item.setName("Lookup maker " + i); manufacturers.save(item);
            }
            var selected = new Manufacturer(); selected.setName("ZZZ lookup selected");
            selected = manufacturers.saveAndFlush(selected);
            assertThat(lookups.manufacturers("", List.of())).hasSize(20);
            assertThat(lookups.manufacturers("", List.of(selected.getId())))
                    .extracting(option -> option.id()).contains(selected.getId());
            assertThat(lookups.manufacturers("ZZZ lookup", List.of())).hasSize(1);
            status.setRollbackOnly();
        });
        mvc.perform(get("/lookups/products").param("query", "x".repeat(101)))
                .andExpect(status().isBadRequest());
        var client = new StorePrincipal(1L, "mercury@gmail.com", "hash", "CLIENT");
        mvc.perform(get("/lookups/clients").with(user(client))).andExpect(status().isForbidden());
    }
}
