package com.epam.rd.autocode.assessment.appliances.integration;

import com.epam.rd.autocode.assessment.appliances.model.Client;
import com.epam.rd.autocode.assessment.appliances.repository.ClientRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:password-change-session;MODE=PostgreSQL;DB_CLOSE_DELAY=-1","store.security.web-auth=session","store.api.enabled=false"})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PasswordChangeSessionIntegrationTest {
    @Autowired MockMvc mvc;@Autowired ClientRepository clients;@Autowired PasswordEncoder encoder;
    @Test void successfulChangeEndsTheCurrentAndOtherTrackedSessionsWithoutJwtInfrastructure() throws Exception {
        String email="pw-session-"+UUID.randomUUID()+"@example.com",old="OldReviewPass2026!",next="NewReviewPass2026!";
        clients.saveAndFlush(new Client(null,"Password session",email,encoder.encode(old),"PW-SESSION"));
        var first=(MockHttpSession)mvc.perform(formLogin().user(email).password(old)).andReturn().getRequest().getSession(false);
        var second=(MockHttpSession)mvc.perform(formLogin().user(email).password(old)).andReturn().getRequest().getSession(false);
        var page=mvc.perform(get("/password/change").session(first)).andExpect(status().isOk()).andReturn();
        var csrf=(CsrfToken)page.getRequest().getAttribute(CsrfToken.class.getName());
        mvc.perform(post("/password/change").session(first).param(csrf.getParameterName(),csrf.getToken())
                .param("currentPassword",old).param("newPassword",next).param("confirmPassword",next))
                .andExpect(redirectedUrl("/login?passwordChanged"));
        mvc.perform(get("/orders").session(second)).andExpect(redirectedUrl("/login?expired"));
    }
}
