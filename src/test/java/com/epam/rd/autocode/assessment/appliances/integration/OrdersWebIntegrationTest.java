package com.epam.rd.autocode.assessment.appliances.integration;

import com.epam.rd.autocode.assessment.appliances.repository.ApplianceRepository;
import com.epam.rd.autocode.assessment.appliances.repository.OrdersRepository;
import com.epam.rd.autocode.assessment.appliances.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("test")
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:orders-web-final"
})
@AutoConfigureMockMvc
class OrdersWebIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ApplianceRepository products;

    @Autowired
    private UserRepository users;

    @Autowired
    private OrdersRepository orders;

    @Test
    void fullOrderLifecycleThroughHttpRespectsRolesOwnershipAndCsrf()
            throws Exception {
        var client = login(
                "mercury@gmail.com",
                "ClientDemo2026!",
                "CLIENT"
        );
        var employee = login(
                "phobos@gmail.com",
                "EmployeeDemo2026!",
                "EMPLOYEE"
        );

        var claw = products.search("Claw", PageRequest.of(0, 1))
                .getContent()
                .get(0);

        var bane = products.search("Bane", PageRequest.of(0, 1))
                .getContent()
                .get(0);

        long venusId = users.findByEmailIgnoreCase("venus@gmail.com")
                .orElseThrow()
                .getId();

        mvc.perform(get("/orders/add").session(client))
                .andExpect(status().isOk())
                .andExpect(content().string(
                        containsString("items[0].applianceId")
                ))
                .andExpect(content().string(
                        not(containsString("name=\"clientId\""))
                ));

        mvc.perform(get("/orders/add").session(employee))
                .andExpect(status().isOk())
                .andExpect(content().string(
                        containsString("name=\"clientId\"")
                ));

        String ownPath = mvc.perform(
                        post("/orders/add")
                                .session(client)
                                .with(csrf())
                                .param(
                                        "items[0].applianceId",
                                        claw.getId().toString()
                                )
                                .param("items[0].quantity", "2")
                                .param(
                                        "items[1].applianceId",
                                        bane.getId().toString()
                                )
                                .param("items[1].quantity", "1")
                )
                .andExpect(status().is3xxRedirection())
                .andReturn()
                .getResponse()
                .getRedirectedUrl();

        assertThat(ownPath).matches("/orders/\\d+");

        long ownId = Long.parseLong(
                ownPath.substring("/orders/".length())
        );

        mvc.perform(get(ownPath).session(client))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("4.03")))
                .andExpect(content().string(
                        not(containsString(ownPath + "/approve"))
                ));

        mvc.perform(get(ownPath + "/edit").session(client))
                .andExpect(status().isOk())
                .andExpect(content().string(
                        containsString("items[1].quantity")
                ));

        mvc.perform(
                        post(ownPath + "/edit")
                                .session(client)
                                .with(csrf())
                                .param(
                                        "items[0].applianceId",
                                        claw.getId().toString()
                                )
                                .param("items[0].quantity", "3")
                )
                .andExpect(redirectedUrl(ownPath));

        assertThat(orders.findById(ownId).orElseThrow().getApproved())
                .isFalse();

        mvc.perform(
                        post(ownPath + "/approve")
                                .session(client)
                                .with(csrf())
                )
                .andExpect(status().isForbidden());

        mvc.perform(
                        post(ownPath + "/approve")
                                .session(employee)
                )
                .andExpect(status().isForbidden());

        mvc.perform(
                        post(ownPath + "/approve")
                                .session(employee)
                                .with(csrf())
                )
                .andExpect(redirectedUrl(ownPath));

        assertThat(orders.findById(ownId).orElseThrow().getApproved())
                .isTrue();

        mvc.perform(
                        get("/orders")
                                .session(employee)
                                .param("status", "approved")
                                .param("sort", "approved")
                )
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(ownPath)));

        mvc.perform(
                        get("/orders")
                                .session(client)
                                .param("status", "pending")
                )
                .andExpect(status().isOk())
                .andExpect(content().string(
                        not(containsString(ownPath))
                ));

        for (MockHttpSession session : List.of(client, employee)) {
            mvc.perform(get(ownPath).session(session))
                    .andExpect(status().isOk())
                    .andExpect(content().string(not(containsString(
                            "href=\"" + ownPath + "/edit\""
                    ))))
                    .andExpect(content().string(not(containsString(
                            "action=\"" + ownPath + "/delete\""
                    ))))
                    .andExpect(content().string(not(containsString(
                            "action=\"" + ownPath + "/approve\""
                    ))));

            mvc.perform(get(ownPath + "/edit").session(session))
                    .andExpect(redirectedUrl("/orders"))
                    .andExpect(flash().attribute("errorKey", "order.not-editable"));

            mvc.perform(post(ownPath + "/edit").session(session).with(csrf())
                            .param("items[0].applianceId", claw.getId().toString())
                            .param("items[0].quantity", "99"))
                    .andExpect(redirectedUrl("/orders"))
                    .andExpect(flash().attribute("errorKey", "order.not-editable"));

            mvc.perform(post(ownPath + "/edit").session(session).with(csrf()))
                    .andExpect(redirectedUrl("/orders"))
                    .andExpect(flash().attribute("errorKey", "order.not-editable"));

            mvc.perform(post(ownPath + "/delete").session(session).with(csrf()))
                    .andExpect(redirectedUrl("/orders"))
                    .andExpect(flash().attribute("errorKey", "order.not-editable"));
        }

        assertThat(orders.findById(ownId).orElseThrow().getApproved()).isTrue();
        assertThat(orders.findDetailedById(ownId).orElseThrow().getOrderRowSet())
                .singleElement().satisfies(row -> {
                    assertThat(row.getNumber()).isEqualTo(3L);
                    assertThat(row.getAmount()).isEqualByComparingTo("3.03");
                });

        mvc.perform(get(ownPath).session(client).param("lang", "uk"))
                .andExpect(content().string(containsString(
                        "Підтверджені замовлення не можна редагувати або видаляти."
                )));
        mvc.perform(get(ownPath).session(client).param("lang", "en"))
                .andExpect(content().string(containsString(
                        "Approved orders cannot be edited or deleted."
                )));

        String otherPath = mvc.perform(
                        post("/orders/add")
                                .session(employee)
                                .with(csrf())
                                .param("clientId", Long.toString(venusId))
                                .param(
                                        "items[0].applianceId",
                                        claw.getId().toString()
                                )
                                .param("items[0].quantity", "1")
                )
                .andExpect(status().is3xxRedirection())
                .andReturn()
                .getResponse()
                .getRedirectedUrl();

        assertThat(otherPath).matches("/orders/\\d+");

        mvc.perform(get(otherPath).session(client))
                .andExpect(redirectedUrl("/orders"))
                .andExpect(flash().attribute(
                        "errorKey",
                        "order.not-found"
                ));

        mvc.perform(
                        post(otherPath + "/delete")
                                .session(client)
                                .with(csrf())
                )
                .andExpect(redirectedUrl("/orders"))
                .andExpect(flash().attribute(
                        "errorKey",
                        "order.not-found"
                ));

        mvc.perform(
                        post(otherPath + "/edit")
                                .session(client)
                                .with(csrf())
                                .param(
                                        "items[0].applianceId",
                                        claw.getId().toString()
                                )
                                .param("items[0].quantity", "99")
                )
                .andExpect(redirectedUrl("/orders"))
                .andExpect(flash().attribute(
                        "errorKey",
                        "order.not-found"
                ));

        long otherId = Long.parseLong(
                otherPath.substring("/orders/".length())
        );

        assertThat(
                orders.findDetailedById(otherId)
                        .orElseThrow()
                        .getOrderRowSet()
        )
                .singleElement()
                .satisfies(row ->
                        assertThat(row.getNumber()).isEqualTo(1L)
                );

        String pendingPath = mvc.perform(
                        post("/orders/add").session(client).with(csrf())
                                .param("items[0].applianceId", claw.getId().toString())
                                .param("items[0].quantity", "1")
                )
                .andExpect(status().is3xxRedirection())
                .andReturn().getResponse().getRedirectedUrl();
        assertThat(pendingPath).matches("/orders/\\d+");
        long pendingId = Long.parseLong(pendingPath.substring("/orders/".length()));

        mvc.perform(post(pendingPath + "/delete").session(client).with(csrf()))
                .andExpect(redirectedUrl("/orders"));
        assertThat(orders.existsById(pendingId)).isFalse();

        orders.deleteById(ownId);

        mvc.perform(
                        post(otherPath + "/delete")
                                .session(employee)
                                .with(csrf())
                )
                .andExpect(redirectedUrl("/orders"));
    }

    @Test
    void rejectsEmptyAndDuplicateItemsWithoutLosingForm()
            throws Exception {
        var client = login(
                "mercury@gmail.com",
                "ClientDemo2026!",
                "CLIENT"
        );

        String applianceId = products.search(
                        "Claw",
                        PageRequest.of(0, 1)
                )
                .getContent()
                .get(0)
                .getId()
                .toString();

        mvc.perform(
                        post("/orders/add")
                                .session(client)
                                .with(csrf())
                )
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors(
                        "orderForm",
                        "items"
                ));

        mvc.perform(
                        post("/orders/add")
                                .session(client)
                                .with(csrf())
                                .param("items[0].applianceId", applianceId)
                                .param("items[0].quantity", "1")
                                .param("items[1].applianceId", applianceId)
                                .param("items[1].quantity", "2")
                )
                .andExpect(status().isOk())
                .andExpect(model().attributeHasErrors("orderForm"))
                .andExpect(content().string(
                        containsString("items[1].quantity")
                ));
    }

    private MockHttpSession login(
            String email,
            String password,
            String role
    ) throws Exception {
        return (MockHttpSession) mvc.perform(
                        formLogin()
                                .user(email)
                                .password(password)
                )
                .andExpect(authenticated().withRoles(role))
                .andReturn()
                .getRequest()
                .getSession(false);
    }
}