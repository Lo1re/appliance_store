package com.epam.rd.autocode.assessment.appliances.integration;

import com.epam.rd.autocode.assessment.appliances.model.Client;
import com.epam.rd.autocode.assessment.appliances.repository.ClientRepository;
import com.epam.rd.autocode.assessment.appliances.repository.LoginAttemptRepository;
import com.epam.rd.autocode.assessment.appliances.security.jwt.AccessTokenCookies;
import com.epam.rd.autocode.assessment.appliances.security.jwt.JwtProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {"spring.datasource.url=jdbc:h2:mem:jwt-cookie;DB_CLOSE_DELAY=-1",
        "store.security.web-auth=jwt", "store.api.enabled=true", "store.api.token-transport=cookie",
        "store.security.jwt.secret-base64=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class JwtCookieIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired LoginAttemptRepository attempts;
    @Autowired ClientRepository clients;
    @Autowired JwtEncoder encoder;
    @Autowired JwtProperties properties;
    @BeforeEach void prepare() { attempts.deleteAll(); }

    @Test
    void ordinaryLoginAuthenticatesWebsiteAndApiWithoutJavaScriptOrSecuritySession() throws Exception {
        var access = login("mercury@gmail.com", "MercuryLocal2026!");
        assertThat(access.isHttpOnly()).isTrue(); assertThat(access.getPath()).isEqualTo("/");
        assertThat(access.getMaxAge()).isBetween(1, 900);
        assertThat(access.getAttribute("SameSite")).isEqualTo("Lax"); assertThat(access.getDomain()).isNull();
        var orders = mvc.perform(get("/orders").cookie(access)).andExpect(status().isOk())
                .andExpect(content().string(not(containsString(access.getValue())))).andReturn();
        assertThat(orders.getRequest().getSession(false)).isNull();
        mvc.perform(get("/api/orders").cookie(access)).andExpect(status().isOk());
        mvc.perform(get("/employees").cookie(access)).andExpect(status().isForbidden());
        var employee = login("phobos@gmail.com", "PhobosLocal2026!");
        mvc.perform(get("/employees").cookie(employee)).andExpect(status().isOk());

        // Neither a copied servlet session nor URL/header credentials authorize HTML pages.
        var session = new MockHttpSession();
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(org.springframework.security.authentication.UsernamePasswordAuthenticationToken.authenticated(
                "forged", null, List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_EMPLOYEE"))));
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
        mvc.perform(get("/employees").session(session)).andExpect(status().is3xxRedirection());
        mvc.perform(get("/orders").param("access_token", access.getValue())).andExpect(status().is3xxRedirection());
        mvc.perform(get("/orders").header("Authorization", "Bearer " + access.getValue())).andExpect(status().is3xxRedirection());
    }

    @Test
    void cookieAuthenticatedHtmlAndApiWritesAndLogoutAlwaysRequireCsrf() throws Exception {
        mvc.perform(post("/login").param("username", "mercury@gmail.com").param("password", "MercuryLocal2026!"))
                .andExpect(status().isForbidden());
        var access = login("mercury@gmail.com", "MercuryLocal2026!");
        mvc.perform(post("/orders/add").cookie(access)).andExpect(status().isForbidden());
        mvc.perform(post("/api/orders").cookie(access).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/logout").cookie(access)).andExpect(status().isForbidden());
        var form = mvc.perform(get("/orders").cookie(access)).andReturn();
        var logout = mvc.perform(post("/logout").cookie(access, form.getResponse().getCookie("STORE-CSRF"))
                        .param("_csrf", csrf(form))).andExpect(redirectedUrl("/login?logout")).andReturn();
        assertThat(logout.getRequest().getSession(false)).isNull();
        assertThat(logout.getResponse().getCookie(AccessTokenCookies.NAME).getMaxAge()).isZero();
        assertThat(logout.getResponse().getCookie(AccessTokenCookies.NAME).getPath()).isEqualTo(access.getPath());
        mvc.perform(get("/orders")).andExpect(status().is3xxRedirection());
        mvc.perform(get("/api/orders")).andExpect(status().isUnauthorized());
        // Clearing cookies is not server-side revocation of a previously copied JWT.
        mvc.perform(get("/api/orders").cookie(access)).andExpect(status().isOk());
    }

    @Test
    void jsonCookieLoginAndOrderWritesRequireCsrfAndDoNotReturnJwtValues() throws Exception {
        String credentials = "{\"email\":\"mercury@gmail.com\",\"password\":\"MercuryLocal2026!\"}";
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(credentials))
                .andExpect(status().isForbidden());
        var bootstrap = mvc.perform(get("/api/auth/csrf")).andExpect(status().isOk()).andReturn();
        var token = json.readTree(bootstrap.getResponse().getContentAsString());
        var login = mvc.perform(post("/api/auth/login").cookie(bootstrap.getResponse().getCookie("STORE-CSRF"))
                        .header(token.path("headerName").asText(), token.path("token").asText())
                        .contentType(MediaType.APPLICATION_JSON).content(credentials))
                .andExpect(status().isOk()).andExpect(jsonPath("$.accessToken").doesNotExist())
                .andExpect(jsonPath("$.expiresAt").exists()).andReturn();
        Cookie access = login.getResponse().getCookie(AccessTokenCookies.NAME);
        assertThat(login.getResponse().getContentAsString()).doesNotContain(access.getValue());
        assertThat(login.getRequest().getSession(false)).isNull();
        // The JSON login issues the same cookie that the regular website accepts.
        mvc.perform(get("/orders").cookie(access)).andExpect(status().isOk());
        var fresh = mvc.perform(get("/api/auth/csrf")).andReturn();
        var freshToken = json.readTree(fresh.getResponse().getContentAsString());
        mvc.perform(post("/api/orders").cookie(access, fresh.getResponse().getCookie("STORE-CSRF"))
                        .header(freshToken.path("headerName").asText(), freshToken.path("token").asText())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void failedAndLockedLoginNeverCreateHttpSessions() throws Exception {
        for (int i = 0; i < 5; i++) assertRejectedPassword("wrong", "/login?error");
        assertRejectedPassword("MercuryLocal2026!", "/login?locked");
    }

    @Test
    void rejectedCookiesPreserveHtmlErrorsAndApiJsonAndCannotBreakTheLoginPage() throws Exception {
        var invalid = new Cookie(AccessTokenCookies.NAME, "private-invalid-jwt");
        var denied = mvc.perform(get("/orders").cookie(invalid)).andExpect(status().is3xxRedirection()).andReturn();
        assertThat(denied.getResponse().getRedirectedUrl()).endsWith("/login");
        assertThat(denied.getResponse().getCookie(AccessTokenCookies.NAME).getMaxAge()).isZero();
        mvc.perform(get("/login").cookie(invalid)).andExpect(status().isOk());
        mvc.perform(get("/definitely-absent-path")).andExpect(status().isNotFound());
        mvc.perform(get("/api/orders").cookie(invalid)).andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(content().string(not(containsString(invalid.getValue()))));
        long id = clients.findAll().get(0).getId();
        var expired = new Cookie(AccessTokenCookies.NAME, signed(id, List.of("CLIENT"), true));
        mvc.perform(get("/orders").cookie(expired)).andExpect(status().is3xxRedirection());
        var changedRole = new Cookie(AccessTokenCookies.NAME, signed(id, List.of("EMPLOYEE"), false));
        mvc.perform(get("/employees").cookie(changedRole)).andExpect(status().is3xxRedirection());
        mvc.perform(get("/api/orders").cookie(changedRole)).andExpect(status().isUnauthorized());
    }

    @Test
    void deletingAccountInvalidatesCookieBeforeAnyUrlAuthorization() throws Exception {
        var client = clients.saveAndFlush(new Client(null, "JWT removal", "jwt-removal@example.com", clients.findAll().get(0).getPassword(), "JWT-REMOVAL"));
        var access = new Cookie(AccessTokenCookies.NAME, signed(client.getId(), List.of("CLIENT"), false));
        mvc.perform(get("/orders").cookie(access)).andExpect(status().isOk());
        clients.deleteById(client.getId()); clients.flush();
        mvc.perform(get("/orders").cookie(access)).andExpect(status().is3xxRedirection());
        mvc.perform(get("/api/orders").cookie(access)).andExpect(status().isUnauthorized());
    }

    private Cookie login(String email, String password) throws Exception {
        var page = mvc.perform(get("/login")).andExpect(status().isOk())
                .andExpect(content().string(not(containsString("<script")))).andReturn();
        assertThat(page.getRequest().getSession(false)).isNull();
        var response = mvc.perform(post("/login").cookie(page.getResponse().getCookie("STORE-CSRF"))
                        .param("_csrf", csrf(page)).param("username", email).param("password", password))
                .andExpect(redirectedUrl("/")).andReturn();
        assertThat(response.getRequest().getSession(false)).isNull();
        var cookie = response.getResponse().getCookie(AccessTokenCookies.NAME); assertThat(cookie).isNotNull(); return cookie;
    }

    private void assertRejectedPassword(String password, String redirect) throws Exception {
        var page = mvc.perform(get("/login")).andReturn();
        var rejected = mvc.perform(post("/login").cookie(page.getResponse().getCookie("STORE-CSRF"))
                        .param("_csrf", csrf(page)).param("username", "mercury@gmail.com").param("password", password))
                .andExpect(redirectedUrl(redirect)).andReturn();
        assertThat(rejected.getRequest().getSession(false)).isNull();
        assertThat(rejected.getResponse().getCookie(AccessTokenCookies.NAME)).isNull();
    }

    private String signed(long id, List<String> roles, boolean expired) {
        var now = Instant.now(); var issued = expired ? now.minusSeconds(1000) : now;
        var claims = JwtClaimsSet.builder().issuer(properties.getIssuer()).audience(List.of(properties.getAudience()))
                .subject(Long.toString(id)).issuedAt(issued).notBefore(issued).expiresAt(issued.plusSeconds(900))
                .id(UUID.randomUUID().toString()).claim("roles", roles).build();
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    }

    private String csrf(MvcResult page) throws Exception {
        var matcher = Pattern.compile("<input(?=[^>]*name=\"_csrf\")(?=[^>]*value=\"([^\"]+)\")[^>]*>")
                .matcher(page.getResponse().getContentAsString());
        assertThat(matcher.find()).isTrue(); return matcher.group(1);
    }
}
