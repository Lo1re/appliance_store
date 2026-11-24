package com.epam.rd.autocode.assessment.appliances.integration;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(
        properties = {
            "spring.datasource.url=jdbc:h2:mem:list-query;DB_CLOSE_DELAY=-1",
            "store.pagination.page-size=3"
        })
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithUserDetails("phobos@gmail.com")
class ListQueryIntegrationTest {
    @Autowired MockMvc mvc;

    @Test
    void appliesConfiguredPageSizeAndSafeSortFallback() throws Exception {
        mvc.perform(get("/appliances").param("page", "-1").param("sort", "password"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("sort", "name"))
                .andExpect(model().attribute("appliances", hasProperty("size", is(3))))
                .andExpect(model().attribute("appliances", hasProperty("number", is(0))));
        mvc.perform(get("/orders"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("sort", "id"));
    }

    @Test
    void rejectsOversizedQueriesAndInvalidPageBinding() throws Exception {
        for (String path :
                new String[] {
                    "/clients", "/employees", "/manufacturers", "/appliances", "/orders"
                }) {
            mvc.perform(get(path).param("search", "x".repeat(101)))
                    .andExpect(status().isBadRequest());
            mvc.perform(get(path).param("page", "not-a-number")).andExpect(status().isBadRequest());
        }
        mvc.perform(get("/manufacturers").param("search", "x".repeat(100)))
                .andExpect(status().isOk());
    }
}
