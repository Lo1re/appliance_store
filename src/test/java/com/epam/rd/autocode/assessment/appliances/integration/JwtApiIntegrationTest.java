package com.epam.rd.autocode.assessment.appliances.integration;

import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.epam.rd.autocode.assessment.appliances.model.Client;
import com.epam.rd.autocode.assessment.appliances.model.Orders;
import com.epam.rd.autocode.assessment.appliances.repository.OrdersRepository;
import com.epam.rd.autocode.assessment.appliances.repository.UserRepository;
import com.epam.rd.autocode.assessment.appliances.repository.security.LoginAttemptRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

@SpringBootTest(
        properties = {
            "spring.datasource.url=jdbc:h2:mem:jwt-api;DB_CLOSE_DELAY=-1",
            "store.api.enabled=true",
            "store.api.token-transport=header",
            "store.security.jwt.secret-base64=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="
        })
@AutoConfigureMockMvc
@ActiveProfiles("test")
class JwtApiIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired org.springframework.context.MessageSource messages;
    @Autowired JwtEncoder encoder;
    @Autowired OrdersRepository orders;
    @Autowired UserRepository users;
    @Autowired LoginAttemptRepository attempts;

    @BeforeEach
    void prepare() {
        orders.deleteAll();
        attempts.deleteAll();
    }

    @Test
    void loginAndBearerRequestsCreateNoHttpSessionAndSessionCookieDoesNotAuthenticateApi()
            throws Exception {
        String token = login("mercury@gmail.com", "MercuryLocal2026!");
        var response =
                mvc.perform(get("/api/appliances").header("Authorization", "Bearer " + token))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.content", not(empty())))
                        .andExpect(jsonPath("$.content[0].password").doesNotExist())
                        .andReturn();
        assertThat(response.getRequest().getSession(false)).isNull();
        assertThat(response.getResponse().getCookie("JSESSIONID")).isNull();
        mvc.perform(get("/api/orders").param("access_token", token))
                .andExpect(status().isUnauthorized());
        mvc.perform(
                        get("/api/orders")
                                .cookie(new jakarta.servlet.http.Cookie("access_token", token)))
                .andExpect(status().isUnauthorized());
        var mvcLogin =
                mvc.perform(formLogin().user("mercury@gmail.com").password("MercuryLocal2026!"))
                        .andReturn();
        var session = (MockHttpSession) mvcLogin.getRequest().getSession(false);
        mvc.perform(get("/api/orders").session(session))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"));
        mvc.perform(post("/orders/add").session(session)).andExpect(status().isForbidden());
        mvc.perform(get("/login").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString(token))));
    }

    @Test
    void invalidTokensAndMalformedInputUseLocalizedSafeProblems() throws Exception {
        mvc.perform(
                        get("/api/orders")
                                .header("Authorization", "Bearer invalid-private-token")
                                .header("Accept-Language", "uk"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Bearer"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("http.401"))
                .andExpect(
                        jsonPath("$.detail")
                                .value(
                                        messages.getMessage(
                                                "error.401.message",
                                                null,
                                                java.util.Locale.forLanguageTag("uk"))))
                .andExpect(content().string(not(containsString("invalid-private-token"))))
                .andExpect(jsonPath("$.trace").doesNotExist());
        String token = login("mercury@gmail.com", "MercuryLocal2026!");
        mvc.perform(
                        get("/api/appliances")
                                .param("size", "101")
                                .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.size").value("invalid"));
        mvc.perform(
                        get("/api/appliances")
                                .param("page", "1000001")
                                .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
        mvc.perform(
                        post("/api/orders")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{broken"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"));
        mvc.perform(get("/api/unknown").header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
        mvc.perform(put("/api/appliances").header("Authorization", "Bearer " + token))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(header().string("Allow", containsString("GET")));
    }

    @Test
    void orderLifecycleKeepsOwnershipServerPricesAndRoleChecks() throws Exception {
        String client = login("mercury@gmail.com", "MercuryLocal2026!");
        String employee = login("phobos@gmail.com", "PhobosLocal2026!");
        long product =
                json.readTree(
                                mvc.perform(
                                                get("/api/appliances")
                                                        .header(
                                                                "Authorization",
                                                                "Bearer " + client))
                                        .andReturn()
                                        .getResponse()
                                        .getContentAsString())
                        .path("content")
                        .get(0)
                        .path("id")
                        .asLong();
        String body =
                "{\"order\":{\"items\":[{\"applianceId\":"
                        + product
                        + ",\"quantity\":2,\"amount\":0}]},\"approved\":true}";
        var created =
                mvc.perform(
                                post("/api/orders")
                                        .header("Authorization", "Bearer " + client)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(body))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.summary.approved").value(false))
                        .andReturn();
        long id =
                json.readTree(created.getResponse().getContentAsString())
                        .path("summary")
                        .path("id")
                        .asLong();
        assertThat(
                        json.readTree(created.getResponse().getContentAsString())
                                .path("total")
                                .decimalValue())
                .isPositive();
        mvc.perform(
                        post("/api/orders/" + id + "/approve")
                                .header("Authorization", "Bearer " + client))
                .andExpect(status().isForbidden());
        mvc.perform(
                        post("/api/orders/" + id + "/approve")
                                .header("Authorization", "Bearer " + employee))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.approved").value(true));
        mvc.perform(delete("/api/orders/" + id).header("Authorization", "Bearer " + client))
                .andExpect(status().isConflict());
        var foreign = new Orders();
        foreign.setClient((Client) users.findByEmailIgnoreCase("venus@gmail.com").orElseThrow());
        long other = orders.saveAndFlush(foreign).getId();
        for (long target : new long[] {other, Long.MAX_VALUE}) {
            mvc.perform(get("/api/orders/" + target).header("Authorization", "Bearer " + client))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("order.not-found"))
                    .andExpect(content().string(not(containsString("venus@gmail.com"))));
        }
    }

    @Test
    void apiPasswordLoginRespectsTheExistingLockAndTokenRequestsDoNotResetIt() throws Exception {
        String token = login("mercury@gmail.com", "MercuryLocal2026!");
        for (int i = 0; i < 5; i++) {
            mvc.perform(
                            post("/api/auth/login")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(
                                            "{\"email\":\"mercury@gmail.com\",\"password\":\"wrong\"}"))
                    .andExpect(status().isUnauthorized());
        }
        mvc.perform(get("/api/orders").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        assertThat(attempts.findById("mercury@gmail.com").orElseThrow().getFailedCount())
                .isEqualTo(5);
        mvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"email\":\"mercury@gmail.com\",\"password\":\"MercuryLocal2026!\"}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.code").value("login.locked"));
    }

    @Test
    void validSignatureCannotAuthorizeADeletedAccountOrContradictItsRole() throws Exception {
        long id = users.findByEmailIgnoreCase("mercury@gmail.com").orElseThrow().getId();
        mvc.perform(
                        get("/api/orders")
                                .header(
                                        "Authorization",
                                        "Bearer " + signed(id, List.of("EMPLOYEE"))))
                .andExpect(status().isUnauthorized());
        mvc.perform(
                        get("/api/orders")
                                .header(
                                        "Authorization",
                                        "Bearer " + signed(id, List.of("CLIENT", "EMPLOYEE"))))
                .andExpect(status().isUnauthorized());
        mvc.perform(
                        get("/api/orders")
                                .header(
                                        "Authorization",
                                        "Bearer " + signed(Long.MAX_VALUE, List.of("CLIENT"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void headerRefreshRotatesAndLogoutRevokesWithoutCookieCredentials() throws Exception {
        var login =
                mvc.perform(
                                post("/api/auth/login")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                "{\"email\":\"mercury@gmail.com\",\"password\":\"MercuryLocal2026!\"}"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.refreshToken").isString())
                        .andReturn();
        String original =
                json.readTree(login.getResponse().getContentAsString())
                        .path("refreshToken")
                        .asText();
        var rotated =
                mvc.perform(
                                post("/api/auth/refresh")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                json.writeValueAsString(
                                                        java.util.Map.of(
                                                                "refreshToken", original))))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.accessToken").isString())
                        .andReturn();
        String next =
                json.readTree(rotated.getResponse().getContentAsString())
                        .path("refreshToken")
                        .asText();
        assertThat(next).isNotEqualTo(original);
        for (int i = 0; i < 2; i++) {
            mvc.perform(
                            post("/api/auth/logout")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(
                                            json.writeValueAsString(
                                                    java.util.Map.of("refreshToken", next))))
                    .andExpect(status().isNoContent());
        }
        mvc.perform(
                        post("/api/auth/refresh")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        json.writeValueAsString(
                                                java.util.Map.of("refreshToken", next))))
                .andExpect(status().isUnauthorized());
    }

    private String login(String email, String password) throws Exception {
        var response =
                mvc.perform(
                                post("/api/auth/login")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                json.writeValueAsString(
                                                        java.util.Map.of(
                                                                "email",
                                                                email,
                                                                "password",
                                                                password))))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.tokenType").value("Bearer"))
                        .andReturn();
        assertThat(response.getRequest().getSession(false)).isNull();
        String token =
                json.readTree(response.getResponse().getContentAsString())
                        .path("accessToken")
                        .asText();
        assertThat(token).isNotBlank();
        return token;
    }

    private String signed(long id, List<String> roles) {
        Instant now = Instant.now();
        var claims =
                JwtClaimsSet.builder()
                        .issuer("appliance-store")
                        .audience(List.of("appliance-store-api"))
                        .subject(Long.toString(id))
                        .issuedAt(now)
                        .expiresAt(now.plusSeconds(900))
                        .id("test-id")
                        .claim("roles", roles)
                        .build();
        return encoder.encode(
                        JwtEncoderParameters.from(
                                JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }
}
