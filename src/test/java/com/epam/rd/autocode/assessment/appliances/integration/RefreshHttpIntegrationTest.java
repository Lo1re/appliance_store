package com.epam.rd.autocode.assessment.appliances.integration;

import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.epam.rd.autocode.assessment.appliances.repository.security.RefreshTokenRepository;
import com.epam.rd.autocode.assessment.appliances.security.jwt.cookie.AuthTokenCookies;
import com.epam.rd.autocode.assessment.appliances.security.jwt.refresh.RefreshTokenCodec;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.Cookie;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(
        properties = {
            "spring.datasource.url=jdbc:h2:mem:refresh-http;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
            "store.security.web-auth=jwt",
            "store.api.enabled=true",
            "store.api.token-transport=cookie",
            "store.security.jwt.secret-base64=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="
        })
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RefreshHttpIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired RefreshTokenRepository tokens;
    @Autowired RefreshTokenCodec codec;
    @Autowired ObjectMapper json;

    @BeforeEach
    void prepare() {
        tokens.deleteAll();
    }

    @Test
    void expiredAccessLeadsToSafeHtmlRenewalAndLogoutRevokesTheRefreshCredential()
            throws Exception {
        var login = login();
        Cookie refresh = login.getResponse().getCookie(AuthTokenCookies.REFRESH_NAME);
        assertThat(refresh).isNotNull();
        assertThat(refresh.isHttpOnly()).isTrue();
        assertThat(refresh.getMaxAge()).isBetween(6 * 86400, 7 * 86400);
        assertThat(refresh.getPath()).isEqualTo("/");
        // Omitting the access cookie simulates its browser expiry; it does not repeat any old POST.
        mvc.perform(get("/orders").cookie(refresh)).andExpect(redirectedUrl("/auth/renew"));
        var page =
                mvc.perform(get("/auth/renew").cookie(refresh))
                        .andExpect(status().isOk())
                        .andExpect(content().string(not(containsString(refresh.getValue()))))
                        .andExpect(content().string(not(containsString("localStorage"))))
                        .andExpect(content().string(not(containsString("document.cookie"))))
                        .andReturn();
        assertThat(page.getRequest().getSession(false)).isNull();
        mvc.perform(post("/auth/renew").cookie(refresh)).andExpect(status().isForbidden());
        var csrf = (CsrfToken) page.getRequest().getAttribute(CsrfToken.class.getName());
        var renewed =
                mvc.perform(
                                post("/auth/renew")
                                        .cookie(refresh, page.getResponse().getCookie("STORE-CSRF"))
                                        .param(csrf.getParameterName(), csrf.getToken()))
                        .andExpect(redirectedUrl("/"))
                        .andReturn();
        Cookie next = renewed.getResponse().getCookie(AuthTokenCookies.REFRESH_NAME);
        Cookie access = renewed.getResponse().getCookie(AuthTokenCookies.NAME);
        assertThat(next.getValue()).isNotEqualTo(refresh.getValue());
        mvc.perform(get("/orders").cookie(access)).andExpect(status().isOk());
        var orders = mvc.perform(get("/orders").cookie(access)).andReturn();
        var logoutCsrf = (CsrfToken) orders.getRequest().getAttribute(CsrfToken.class.getName());
        var logout =
                mvc.perform(
                                post("/logout")
                                        .cookie(
                                                access,
                                                next,
                                                orders.getResponse().getCookie("STORE-CSRF"))
                                        .param(
                                                logoutCsrf.getParameterName(),
                                                logoutCsrf.getToken()))
                        .andExpect(redirectedUrl("/login?logout"))
                        .andReturn();
        assertThat(logout.getResponse().getCookie(AuthTokenCookies.REFRESH_NAME).getMaxAge())
                .isZero();
        assertThat(tokens.findByTokenHash(codec.hash(next.getValue())).orElseThrow().getRevokedAt())
                .isNotNull();
    }

    @Test
    void jsonRefreshUsesCsrfRotatesTheCookieAndNeverReturnsEitherCredential() throws Exception {
        var login = login();
        Cookie refresh = login.getResponse().getCookie(AuthTokenCookies.REFRESH_NAME);
        mvc.perform(post("/api/auth/refresh").cookie(refresh)).andExpect(status().isForbidden());
        var bootstrap = mvc.perform(get("/api/auth/csrf")).andReturn();
        var csrf = json.readTree(bootstrap.getResponse().getContentAsString());
        var renewed =
                mvc.perform(
                                post("/api/auth/refresh")
                                        .cookie(
                                                refresh,
                                                bootstrap.getResponse().getCookie("STORE-CSRF"))
                                        .header(
                                                csrf.path("headerName").asText(),
                                                csrf.path("token").asText()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.accessToken").doesNotExist())
                        .andExpect(jsonPath("$.refreshToken").doesNotExist())
                        .andReturn();
        Cookie next = renewed.getResponse().getCookie(AuthTokenCookies.REFRESH_NAME);
        assertThat(renewed.getRequest().getSession(false)).isNull();
        assertThat(renewed.getResponse().getContentAsString())
                .doesNotContain(next.getValue(), refresh.getValue());
        var bootstrapAgain = mvc.perform(get("/api/auth/csrf")).andReturn();
        var csrfAgain = json.readTree(bootstrapAgain.getResponse().getContentAsString());
        // Replay remains an API error; all refresh access is revoked in the committed transaction.
        var replay =
                mvc.perform(
                                post("/api/auth/refresh")
                                        .cookie(
                                                refresh,
                                                bootstrapAgain
                                                        .getResponse()
                                                        .getCookie("STORE-CSRF"))
                                        .header(
                                                csrfAgain.path("headerName").asText(),
                                                csrfAgain.path("token").asText()))
                        .andExpect(status().isUnauthorized())
                        .andExpect(jsonPath("$.code").value("refresh.invalid"))
                        .andReturn();
        assertThat(replay.getResponse().getCookie(AuthTokenCookies.REFRESH_NAME).getMaxAge())
                .isZero();
        assertThat(tokens.findByTokenHash(codec.hash(next.getValue())).orElseThrow().getRevokedAt())
                .isNotNull();
    }

    @Test
    void authenticatedBackgroundRequestsDoNotDeleteCsrfOrBreakLogout() throws Exception {
        var login = login();
        Cookie access = login.getResponse().getCookie(AuthTokenCookies.NAME);
        Cookie refresh = login.getResponse().getCookie(AuthTokenCookies.REFRESH_NAME);
        var page =
                mvc.perform(get("/orders").cookie(access, refresh))
                        .andExpect(status().isOk())
                        .andReturn();
        Cookie csrfCookie = page.getResponse().getCookie("STORE-CSRF");
        assertThat(csrfCookie).isNotNull();
        var csrf = (CsrfToken) page.getRequest().getAttribute(CsrfToken.class.getName());
        // This is the browser's original form and cookie, retained while other requests run.
        for (String path :
                new String[] {
                    "/api/orders", "/.well-known/appspecific/com.chrome.devtools.json", "/"
                }) {
            var background =
                    mvc.perform(get(path).cookie(access, refresh, csrfCookie))
                            .andExpect(status().is(path.contains(".well-known") ? 404 : 200))
                            .andReturn();
            assertThat(background.getResponse().getCookie("STORE-CSRF"))
                    .as(
                            "Ordinary authenticated GET must not replace or delete the browser CSRF"
                                    + " cookie")
                    .isNull();
            assertThat(background.getRequest().getSession(false)).isNull();
        }
        var logout =
                mvc.perform(
                                post("/logout")
                                        .cookie(access, refresh, csrfCookie)
                                        .param(csrf.getParameterName(), csrf.getToken()))
                        .andExpect(redirectedUrl("/login?logout"))
                        .andReturn();
        assertThat(logout.getResponse().getCookie(AuthTokenCookies.NAME).getMaxAge()).isZero();
        assertThat(logout.getResponse().getCookie(AuthTokenCookies.REFRESH_NAME).getMaxAge())
                .isZero();
        assertThat(
                        tokens.findByTokenHash(codec.hash(refresh.getValue()))
                                .orElseThrow()
                                .getRevokedAt())
                .isNotNull();
    }

    @Test
    void repeatingBrowserPasswordLoginReplacesRatherThanAbandonsThePreviousRefresh()
            throws Exception {
        var first = login();
        Cookie previous = first.getResponse().getCookie(AuthTokenCookies.REFRESH_NAME);
        var page = mvc.perform(get("/login").cookie(previous)).andReturn();
        var csrf = (CsrfToken) page.getRequest().getAttribute(CsrfToken.class.getName());
        var replacement =
                mvc.perform(
                                post("/login")
                                        .cookie(
                                                previous,
                                                page.getResponse().getCookie("STORE-CSRF"))
                                        .param(csrf.getParameterName(), csrf.getToken())
                                        .param("username", "mercury@gmail.com")
                                        .param("password", "MercuryLocal2026!"))
                        .andExpect(redirectedUrl("/"))
                        .andReturn();
        Cookie next = replacement.getResponse().getCookie(AuthTokenCookies.REFRESH_NAME);
        assertThat(next.getValue()).isNotEqualTo(previous.getValue());
        assertThat(
                        tokens.findByTokenHash(codec.hash(previous.getValue()))
                                .orElseThrow()
                                .getRevokedAt())
                .isNotNull();
        assertThat(tokens.findByTokenHash(codec.hash(next.getValue())).orElseThrow().getRevokedAt())
                .isNull();
    }

    private MvcResult login() throws Exception {
        var page = mvc.perform(get("/login")).andReturn();
        var csrf = (CsrfToken) page.getRequest().getAttribute(CsrfToken.class.getName());
        return mvc.perform(
                        post("/login")
                                .cookie(page.getResponse().getCookie("STORE-CSRF"))
                                .param(csrf.getParameterName(), csrf.getToken())
                                .param("username", "mercury@gmail.com")
                                .param("password", "MercuryLocal2026!"))
                .andExpect(redirectedUrl("/"))
                .andReturn();
    }
}
