package com.epam.rd.autocode.assessment.appliances.controller.auth;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:login-page-smoke"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LoginControllerTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void rendersLoginInBothLanguagesWithCsrf() throws Exception {
        mvc.perform(get("/login").param("lang", "uk"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Увійти")))
                .andExpect(content().string(
                        containsString("name=\"_csrf\"")
                ));

        mvc.perform(get("/login").param("lang", "en"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Sign in")));
    }

    @Test
    void authenticatesBothRolesAndLocalizesFailedLoginAndLogout()
            throws Exception {
        mvc.perform(formLogin()
                        .user("mercury@gmail.com")
                        .password("wrong"))
                .andExpect(unauthenticated())
                .andExpect(redirectedUrl("/login?error"));

        mvc.perform(get("/login")
                        .param("error", "")
                        .cookie(new Cookie("store-lang", "uk")))
                .andExpect(content().string(
                        containsString("Неправильний логін або пароль.")
                ));

        mvc.perform(formLogin()
                        .user("mercury@gmail.com")
                        .password("MercuryLocal2026!"))
                .andExpect(authenticated().withRoles("CLIENT"));

        var session = (MockHttpSession) mvc.perform(formLogin()
                        .user("phobos@gmail.com")
                        .password("PhobosLocal2026!"))
                .andExpect(authenticated().withRoles("EMPLOYEE"))
                .andReturn()
                .getRequest()
                .getSession(false);

        mvc.perform(post("/logout")
                        .session(session)
                        .with(csrf()))
                .andExpect(redirectedUrl("/login?logout"));

        mvc.perform(get("/login")
                        .param("logout", "")
                        .cookie(new Cookie("store-lang", "uk")))
                .andExpect(content().string(
                        containsString("Ви вийшли з облікового запису.")
                ));
    }
}
