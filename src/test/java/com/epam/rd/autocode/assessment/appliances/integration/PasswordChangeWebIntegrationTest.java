package com.epam.rd.autocode.assessment.appliances.integration;

import com.epam.rd.autocode.assessment.appliances.model.Client;
import com.epam.rd.autocode.assessment.appliances.repository.ClientRepository;
import com.epam.rd.autocode.assessment.appliances.security.jwt.cookie.AuthTokenCookies;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:password-change-web;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "store.security.web-auth=jwt","store.api.enabled=true",
        "store.security.jwt.secret-base64=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PasswordChangeWebIntegrationTest {
    private static final String OLD="OldReviewPass2026!",NEW="NewReviewPass2026!";
    @Autowired MockMvc mvc;@Autowired ClientRepository clients;@Autowired PasswordEncoder encoder;@Autowired ObjectMapper json;
    private String email;
    @BeforeEach void prepare(){email="pw-web-"+UUID.randomUUID()+"@example.com";clients.saveAndFlush(new Client(null,"Password web",email,encoder.encode(OLD),"PW-WEB"));}

    @Test void ordinaryFormChangesPasswordClearsCookiesAndRevokesRefreshWithoutEchoingSecrets() throws Exception {
        var login=login(OLD);Cookie access=login.getResponse().getCookie(AuthTokenCookies.NAME);Cookie refresh=login.getResponse().getCookie(AuthTokenCookies.REFRESH_NAME);
        var page=mvc.perform(get("/password/change").cookie(access)).andExpect(status().isOk()).andReturn();
        assertThat(page.getRequest().getSession(false)).isNull();
        mvc.perform(post("/password/change").cookie(access).param("currentPassword",OLD).param("newPassword",NEW).param("confirmPassword",NEW)).andExpect(status().isForbidden());
        var csrf=(CsrfToken)page.getRequest().getAttribute(CsrfToken.class.getName());
        var result=mvc.perform(post("/password/change").cookie(access,page.getResponse().getCookie("STORE-CSRF"))
                        .param(csrf.getParameterName(),csrf.getToken()).param("currentPassword",OLD).param("newPassword",NEW).param("confirmPassword",NEW))
                .andExpect(redirectedUrl("/login?passwordChanged")).andReturn();
        assertThat(result.getResponse().getCookie(AuthTokenCookies.NAME).getMaxAge()).isZero();
        assertThat(result.getResponse().getCookie(AuthTokenCookies.REFRESH_NAME).getMaxAge()).isZero();
        assertThat(result.getRequest().getSession(false)).isNull();
        assertThat(login(OLD).getResponse().getCookie(AuthTokenCookies.NAME)).isNull();
        assertThat(login(NEW).getResponse().getCookie(AuthTokenCookies.NAME)).isNotNull();
        var bootstrap=mvc.perform(get("/api/auth/csrf")).andReturn();var token=json.readTree(bootstrap.getResponse().getContentAsString());
        mvc.perform(post("/api/auth/refresh").cookie(refresh,bootstrap.getResponse().getCookie("STORE-CSRF"))
                .header(token.path("headerName").asText(),token.path("token").asText())).andExpect(status().isUnauthorized());
        mvc.perform(get("/login").param("passwordChanged","").param("lang","uk"))
                .andExpect(content().string(containsString("Пароль змінено")));
    }

    @Test void invalidConfirmationAndWrongCurrentPasswordShowFieldErrorsWithoutValues() throws Exception {
        Cookie access=login(OLD).getResponse().getCookie(AuthTokenCookies.NAME);
        for(boolean mismatch:new boolean[]{true,false}){
            var page=mvc.perform(get("/password/change").cookie(access)).andReturn();var csrf=(CsrfToken)page.getRequest().getAttribute(CsrfToken.class.getName());
            var response=mvc.perform(post("/password/change").cookie(access,page.getResponse().getCookie("STORE-CSRF"))
                    .param(csrf.getParameterName(),csrf.getToken()).param("currentPassword",mismatch?OLD:"wrong-private-value")
                    .param("newPassword",NEW).param("confirmPassword",mismatch?"DifferentReviewPass2026!":NEW))
                    .andExpect(status().isBadRequest()).andExpect(model().attributeHasFieldErrors("passwordForm",mismatch?"confirmPassword":"currentPassword")).andReturn();
            assertThat(response.getResponse().getContentAsString()).doesNotContain(OLD,NEW,"DifferentReviewPass2026!","wrong-private-value");
            assertThat(response.getRequest().getSession(false)).isNull();
        }
    }

    @Test void anonymousUsersCannotOpenOrSubmitPasswordChange() throws Exception {
        mvc.perform(get("/password/change")).andExpect(status().is3xxRedirection());
        mvc.perform(post("/password/change").param("currentPassword",OLD).param("newPassword",NEW).param("confirmPassword",NEW))
                .andExpect(status().isForbidden());
    }

    private MvcResult login(String password) throws Exception {
        var page=mvc.perform(get("/login")).andReturn();var csrf=(CsrfToken)page.getRequest().getAttribute(CsrfToken.class.getName());
        return mvc.perform(post("/login").cookie(page.getResponse().getCookie("STORE-CSRF"))
                .param(csrf.getParameterName(),csrf.getToken()).param("username",email).param("password",password)).andReturn();
    }
}
