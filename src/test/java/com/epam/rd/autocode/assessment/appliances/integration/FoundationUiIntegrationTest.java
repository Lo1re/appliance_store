package com.epam.rd.autocode.assessment.appliances.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:foundation-ui;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FoundationUiIntegrationTest {
    @Autowired MockMvc mvc;

    @Test
    @WithUserDetails("phobos@gmail.com")
    void allCurrentFormsRenderTheSharedLanguageNavigation() throws Exception {
        for (String path : new String[]{"/", "/appliances", "/appliances/add", "/manufacturers", "/manufacturers/add",
                "/clients", "/clients/add", "/employees", "/employees/add", "/orders", "/orders/add"}) {
            mvc.perform(get(path)).andExpect(status().isOk())
                    .andExpect(content().string(containsString("hreflang=\"uk\"")))
                    .andExpect(content().string(not(containsString("??"))));
        }
    }

    @Test
    void expiredSessionMessageIsLocalized() throws Exception {
        mvc.perform(get("/login").param("expired", "").param("lang", "uk"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Ваш сеанс завершено. Увійдіть повторно.")));
    }
}
