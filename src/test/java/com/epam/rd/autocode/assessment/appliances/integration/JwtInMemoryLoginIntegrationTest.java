package com.epam.rd.autocode.assessment.appliances.integration;

import com.epam.rd.autocode.assessment.appliances.security.jwt.AccessTokenCookies;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {"spring.datasource.url=jdbc:h2:mem:jwt-memory;DB_CLOSE_DELAY=-1",
        "store.security.web-auth=jwt", "store.api.enabled=false", "store.security.auth-mode=inmemory",
        "store.security.jwt.secret-base64=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
        "store.security.inmemory.accounts[0].email=mercury@gmail.com",
        "store.security.inmemory.accounts[0].password-hash=$2a$10$PsRnmS7huldXCKf0kWLObOB7ndzQZaCC3Bt/gO6X4kohWInxkC//W",
        "store.security.inmemory.accounts[0].role=CLIENT"})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class JwtInMemoryLoginIntegrationTest {
    @Autowired MockMvc mvc;

    @Test
    void sameLoginIssuesJwtFromMemoryCredentialsEvenWhenJsonApiIsDisabled() throws Exception {
        passwordLogin("MercuryLocal2026!")
                .andExpect(redirectedUrl("/login?error"));
        var login = passwordLogin("PhobosLocal2026!")
                .andExpect(redirectedUrl("/")).andReturn();
        assertThat(login.getRequest().getSession(false)).isNull();
        var cookie = login.getResponse().getCookie(AccessTokenCookies.NAME); assertThat(cookie).isNotNull();
        mvc.perform(get("/orders").cookie(cookie)).andExpect(status().isOk());
        mvc.perform(get("/employees").cookie(cookie)).andExpect(status().isForbidden());
        mvc.perform(get("/api/orders").cookie(cookie)).andExpect(status().isNotFound());
    }
    private org.springframework.test.web.servlet.ResultActions passwordLogin(String password) throws Exception {
        var page = mvc.perform(get("/login")).andExpect(status().isOk()).andReturn();
        var csrf = (org.springframework.security.web.csrf.CsrfToken) page.getRequest()
                .getAttribute(org.springframework.security.web.csrf.CsrfToken.class.getName());
        return mvc.perform(post("/login").cookie(page.getResponse().getCookie("STORE-CSRF"))
                .param(csrf.getParameterName(), csrf.getToken()).param("username", "mercury@gmail.com").param("password", password));
    }

}
