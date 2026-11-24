package com.epam.rd.autocode.assessment.appliances.integration;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

import com.epam.rd.autocode.assessment.appliances.model.Client;
import com.epam.rd.autocode.assessment.appliances.model.Orders;
import com.epam.rd.autocode.assessment.appliances.repository.OrdersRepository;
import com.epam.rd.autocode.assessment.appliances.repository.UserRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:resource-privacy;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ResourcePrivacyIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired OrdersRepository orders;
    @Autowired UserRepository users;

    @Test
    @WithUserDetails("mercury@gmail.com")
    void foreignAndAbsentOrdersHaveTheSamePublic404ForReadsAndWrites() throws Exception {
        Long foreign = foreignOrder();
        try {
            same(get("/orders/" + foreign), get("/orders/" + Long.MAX_VALUE), 404);
            same(
                    get("/orders/" + foreign + "/edit"),
                    get("/orders/" + Long.MAX_VALUE + "/edit"),
                    404);
            same(
                    post("/orders/" + foreign + "/edit").with(csrf()),
                    post("/orders/" + Long.MAX_VALUE + "/edit").with(csrf()),
                    404);
            same(
                    post("/orders/" + foreign + "/delete").with(csrf()),
                    post("/orders/" + Long.MAX_VALUE + "/delete").with(csrf()),
                    404);
            assertThat(orders.existsById(foreign)).isTrue();
        } finally {
            orders.deleteById(foreign);
        }
    }

    @Test
    @WithUserDetails("mercury@gmail.com")
    void roleAndCsrfDenialsDoNotDependOnWhetherTheResourceExists() throws Exception {
        Long foreign = foreignOrder();
        try {
            same(
                    post("/orders/" + foreign + "/approve").with(csrf()),
                    post("/orders/" + Long.MAX_VALUE + "/approve").with(csrf()),
                    403);
            same(
                    post("/orders/" + foreign + "/delete"),
                    post("/orders/" + Long.MAX_VALUE + "/delete"),
                    403);
            same(get("/clients/1/edit"), get("/clients/" + Long.MAX_VALUE + "/edit"), 403);
        } finally {
            orders.deleteById(foreign);
        }
    }

    private Long foreignOrder() {
        var order = new Orders();
        order.setClient((Client) users.findByEmailIgnoreCase("venus@gmail.com").orElseThrow());
        return orders.saveAndFlush(order).getId();
    }

    private void same(
            MockHttpServletRequestBuilder first, MockHttpServletRequestBuilder second, int status)
            throws Exception {
        var one = mvc.perform(first).andReturn().getResponse();
        var two = mvc.perform(second).andReturn().getResponse();
        assertThat(one.getStatus()).isEqualTo(status);
        assertThat(two.getStatus()).isEqualTo(status);
        assertThat(one.getContentAsString())
                .isEqualTo(two.getContentAsString())
                .doesNotContain("venus@gmail.com", "Venus", "OrderException", "org.hibernate");
        assertThat(one.getHeader("Location")).isEqualTo(two.getHeader("Location"));
        assertThat(one.getHeader("Cache-Control")).contains("no-store");
        assertThat(two.getHeader("Cache-Control")).contains("no-store");
    }
}
