package com.epam.rd.autocode.assessment.appliances.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:home-web-final",
        "store.demo.client-password=ClientTest2026!",
        "store.demo.employee-password=EmployeeTest2026!"
})
@AutoConfigureMockMvc
class IndexControllerTest {

    @Autowired private MockMvc mvc;

    @Test
    void guestSeesLocalizedHomeAndLoginWithoutManagementLinks() throws Exception {
        mvc.perform(get("/").param("lang", "uk"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(content().string(containsString("Магазин побутової техніки")))
                .andExpect(content().string(containsString("href=\"/login\"")))
                .andExpect(content().string(not(containsString("href=\"/clients\""))))
                .andExpect(content().string(not(containsString("href=\"/orders\""))));
    }

    @Test
    void menuShowsAccountManagementOnlyForEmployees() throws Exception {
        var client = login("client", "ClientTest2026!", "CLIENT");
        var employee = login("employee", "EmployeeTest2026!", "EMPLOYEE");

        mvc.perform(get("/").session(client))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("href=\"/orders\"")))
                .andExpect(content().string(not(containsString("href=\"/employees\""))));
        mvc.perform(get("/").session(employee))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("href=\"/clients\"")))
                .andExpect(content().string(containsString("href=\"/employees\"")))
                .andExpect(content().string(containsString("action=\"/logout\"")));
    }

    private MockHttpSession login(String username, String password, String role) throws Exception {
        return (MockHttpSession) mvc.perform(formLogin().user(username).password(password))
                .andExpect(authenticated().withRoles(role)).andReturn().getRequest().getSession(false);
    }
}
