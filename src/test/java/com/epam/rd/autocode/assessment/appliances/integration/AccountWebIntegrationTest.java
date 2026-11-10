package com.epam.rd.autocode.assessment.appliances.integration;

import com.epam.rd.autocode.assessment.appliances.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:account-web-smoke",
        "store.demo.client-password=ClientTest2026!",
        "store.demo.employee-password=EmployeeTest2026!"
})
@AutoConfigureMockMvc
class AccountWebIntegrationTest {

    private static final String PASSWORD = "OnlyForTest2026!";

    @Autowired private MockMvc mvc;
    @Autowired private UserRepository users;
    @Autowired private PasswordEncoder encoder;

    @ParameterizedTest
    @CsvSource({"/clients,card,WEB-CARD,web-client@example.com", "/employees,department,Stock,web-employee@example.com"})
    void rendersAndPersistsAccountLifecycle(String path, String extraField, String extraValue, String email)
            throws Exception {
        var employee = login("employee", "EmployeeTest2026!", "EMPLOYEE");
        mvc.perform(get(path).session(employee))
                .andExpect(status().isOk())
                .andExpect(view().name("account/list"));
        mvc.perform(get(path + "/add").session(employee))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("name=\"" + extraField + "\"")))
                .andExpect(content().string(containsString("name=\"_csrf\"")));

        mvc.perform(post(path + "/add").session(employee).with(csrf())
                        .param("name", " Test Account ").param("email", email)
                        .param(extraField, extraValue).param("password", PASSWORD))
                .andExpect(redirectedUrl(path));

        var created = users.findByEmailIgnoreCase(email).orElseThrow();
        var id = created.getId();
        String hash = created.getPassword();
        assertThat(hash).isNotEqualTo(PASSWORD);
        assertThat(encoder.matches(PASSWORD, hash)).isTrue();

        mvc.perform(get(path + "/" + id + "/edit").session(employee))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(email)))
                .andExpect(content().string(not(containsString("name=\"password\""))));

        mvc.perform(post(path + "/" + id + "/edit").session(employee).with(csrf())
                        .param("name", "Edited Account").param("email", email).param(extraField, extraValue))
                .andExpect(redirectedUrl(path));

        var updated = users.findById(id).orElseThrow();
        assertThat(updated.getName()).isEqualTo("Edited Account");
        assertThat(updated.getPassword()).isEqualTo(hash);

        mvc.perform(post(path + "/" + id + "/delete").session(employee).with(csrf()))
                .andExpect(redirectedUrl(path));
        assertThat(users.existsById(id)).isFalse();
    }

    @Test
    void displaysLocalizedDuplicateAndPasswordErrorsWithoutEchoingPassword() throws Exception {
        var employee = login("employee", "EmployeeTest2026!", "EMPLOYEE");
        mvc.perform(get("/clients").session(employee).param("lang", "uk"))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Клієнти")));

        mvc.perform(post("/clients/add").session(employee).with(csrf())
                        .param("name", "Duplicate").param("email", "PHOBOS@gmail.com")
                        .param("card", "TEST-CARD").param("password", PASSWORD))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrorCode("account", "email", "user.email.duplicate"))
                .andExpect(content().string(not(containsString(PASSWORD))));

        String tooLong = "A1!" + "x".repeat(70);
        mvc.perform(post("/employees/add").session(employee).with(csrf())
                        .param("name", "Bad Password").param("email", "invalid-password@example.com")
                        .param("department", "Stock").param("password", tooLong))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("credentials", "withinBcryptLimit"))
                .andExpect(content().string(containsString("The password must not exceed 72 UTF-8 bytes.")))
                .andExpect(content().string(not(containsString(tooLong))));
        assertThat(users.existsByEmailIgnoreCase("invalid-password@example.com")).isFalse();
    }

    @Test
    void enforcesEmployeeRoleAndCsrfIndependently() throws Exception {
        var client = login("client", "ClientTest2026!", "CLIENT");
        var employee = login("employee", "EmployeeTest2026!", "EMPLOYEE");
        mvc.perform(get("/clients").session(client)).andExpect(status().isForbidden());
        mvc.perform(get("/employees").session(client)).andExpect(status().isForbidden());
        mvc.perform(post("/clients/add").session(client).with(csrf())).andExpect(status().isForbidden());
        mvc.perform(post("/employees/add").session(employee)).andExpect(status().isForbidden());
    }

    private MockHttpSession login(String username, String password, String role) throws Exception {
        return (MockHttpSession) mvc.perform(formLogin().user(username).password(password))
                .andExpect(authenticated().withRoles(role)).andReturn().getRequest().getSession(false);
    }
}

