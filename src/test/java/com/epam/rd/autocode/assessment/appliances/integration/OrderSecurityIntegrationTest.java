package com.epam.rd.autocode.assessment.appliances.integration;

import com.epam.rd.autocode.assessment.appliances.config.StorePrincipal;
import com.epam.rd.autocode.assessment.appliances.dto.order.OrderItemRequest;
import com.epam.rd.autocode.assessment.appliances.dto.order.OrderRequest;
import com.epam.rd.autocode.assessment.appliances.exception.OrderException;
import com.epam.rd.autocode.assessment.appliances.model.OrderRow;
import com.epam.rd.autocode.assessment.appliances.repository.ApplianceInOrderRepository;
import com.epam.rd.autocode.assessment.appliances.repository.ApplianceRepository;
import com.epam.rd.autocode.assessment.appliances.repository.OrdersRepository;
import com.epam.rd.autocode.assessment.appliances.repository.UserRepository;
import com.epam.rd.autocode.assessment.appliances.service.OrderService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.security.web.context.HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@AutoConfigureMockMvc
@SpringBootTest(useMainMethod = SpringBootTest.UseMainMethod.ALWAYS, properties = {
        "spring.datasource.url=jdbc:h2:mem:order-security-it",
        "store.demo.client-password=ClientTest2026!",
        "store.demo.employee-password=EmployeeTest2026!"
})
class OrderSecurityIntegrationTest {

    private static final long MISSING = Long.MAX_VALUE;
    private static final PageRequest PAGE = PageRequest.of(0, 20);

    @Autowired MockMvc mvc;
    @Autowired OrderService service;
    @Autowired UserRepository users;
    @Autowired ApplianceRepository appliances;
    @Autowired ApplianceInOrderRepository rows;
    @Autowired OrdersRepository orders;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void enforcesLoginRolesAndCsrf() throws Exception {
        mvc.perform(get("/orders"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/login"));

        mvc.perform(formLogin().user("client").password("wrong"))
                .andExpect(unauthenticated());
        mvc.perform(formLogin().user("unknown").password("wrong"))
                .andExpect(unauthenticated());

        MockHttpSession client = login("client", "ClientTest2026!", "CLIENT");
        MockHttpSession employee = login("employee", "EmployeeTest2026!", "EMPLOYEE");

        mvc.perform(get("/clients").session(client))
                .andExpect(status().isForbidden());

        mvc.perform(get("/manufacturers/add").session(client))
                .andExpect(status().isForbidden());

        mvc.perform(post("/orders").session(client))
                .andExpect(status().isForbidden());

        mvc.perform(get("/manufacturers/add").session(employee))
                .andExpect(status().isOk())
                .andExpect(view().name("manufacture/newManufacturer"));
    }

    @Test
    void orderLifecycleEnforcesOwnershipAndPersistsPricesAndRows() throws Exception {
        MockHttpSession client = login("client", "ClientTest2026!", "CLIENT");
        MockHttpSession employee = login("employee", "EmployeeTest2026!", "EMPLOYEE");
        Long venusId = users.findByEmailIgnoreCase("venus@gmail.com").orElseThrow().getId();
        Long applianceId = appliances.search("Claw", PAGE).getContent().get(0).getId();
        OrderRequest twoItems = request(item(applianceId, 2L));

        use(employee);
        var other = service.create(venusId, twoItems);
        assertThat(other.summary().clientId()).isEqualTo(venusId);

        use(client);
        var own = service.create(null, twoItems);
        Long ownId = own.summary().id();
        Long otherId = other.summary().id();

        assertThat(own.summary().clientId()).isEqualTo(principal(client).getAccountId());
        assertThat(own.summary().approved()).isFalse();
        assertThat(service.findById(ownId).total()).isEqualByComparingTo("2.02");
        assertThat(service.findAll(null, PAGE))
                .extracting(summary -> summary.id()).containsExactly(ownId);
        assertThat(service.findAll(false, PAGE))
                .extracting(summary -> summary.id()).containsExactly(ownId);

        assertThatThrownBy(() -> service.findById(otherId))
                .isInstanceOf(OrderException.class).hasMessage("order.not-found");
        assertThatThrownBy(() -> service.update(otherId, twoItems))
                .isInstanceOf(OrderException.class).hasMessage("order.not-found");
        assertThatThrownBy(() -> service.delete(otherId))
                .isInstanceOf(OrderException.class).hasMessage("order.not-found");
        assertThatThrownBy(() -> service.approve(ownId))
                .isInstanceOf(AccessDeniedException.class);

        use(employee);
        assertThat(service.findAll(null, PAGE))
                .extracting(summary -> summary.id()).contains(ownId, otherId);
        assertThat(service.findById(otherId).total()).isEqualByComparingTo("2.02");

        var approved = service.approve(ownId);
        assertThat(approved.summary().approved()).isTrue();
        assertThat(approved.summary().employeeId()).isEqualTo(principal(employee).getAccountId());
        assertThat(service.findAll(true, PAGE))
                .extracting(summary -> summary.id()).containsExactly(ownId);
        assertThatThrownBy(() -> service.approve(ownId))
                .isInstanceOf(OrderException.class).hasMessage("order.already-approved");

        use(client);
        Long oldRowId = own.items().get(0).id();
        var updated = service.update(ownId, request(item(applianceId, 3L)));
        Long newRowId = updated.items().get(0).id();

        assertThat(updated.summary().approved()).isFalse();
        assertThat(updated.summary().employeeId()).isNull();
        assertThat(newRowId).isNotNull().isNotEqualTo(oldRowId);
        assertThat(rows.existsById(oldRowId)).isFalse();
        assertThat(rows.findById(newRowId)).get()
                .extracting(OrderRow::getAmount).isEqualTo(new BigDecimal("3.03"));
        assertThat(service.findById(ownId).total()).isEqualByComparingTo("3.03");

        service.delete(ownId);
        assertThat(orders.existsById(ownId)).isFalse();
        assertThat(rows.existsById(newRowId)).isFalse();

        use(employee);
        service.delete(otherId);
    }

    @Test
    void rejectsBadClientIdsMissingProductsAndDuplicateRows() throws Exception {
        MockHttpSession client = login("client", "ClientTest2026!", "CLIENT");
        MockHttpSession employee = login("employee", "EmployeeTest2026!", "EMPLOYEE");
        Long applianceId = appliances.search("Claw", PAGE).getContent().get(0).getId();
        var valid = request(item(applianceId, 1L));

        use(client);
        var own = service.create(principal(client).getAccountId(), valid);

        assertThatThrownBy(() -> service.create(MISSING, valid))
                .hasMessage("order.access-denied");
        assertThatThrownBy(() -> service.create(null, request(item(MISSING, 1L))))
                .hasMessage("order.appliance.not-found");
        assertThatThrownBy(() -> service.create(
                null, request(item(applianceId, 1L), item(applianceId, 2L))))
                .hasMessage("order.appliance.duplicate");
        assertThatThrownBy(() -> service.findById(MISSING)).hasMessage("order.not-found");
        assertThatThrownBy(() -> service.delete(MISSING)).hasMessage("order.not-found");

        service.delete(own.summary().id());

        use(employee);
        assertThatThrownBy(() -> service.create(null, valid)).hasMessage("order.client.required");
        assertThatThrownBy(() -> service.create(MISSING, valid)).hasMessage("order.client.not-found");
    }

    private MockHttpSession login(String username, String password, String role) throws Exception {
        return (MockHttpSession) mvc.perform(formLogin().user(username).password(password))
                .andExpect(authenticated().withRoles(role))
                .andReturn().getRequest().getSession(false);
    }

    private StorePrincipal principal(MockHttpSession session) {
        return (StorePrincipal) context(session).getAuthentication().getPrincipal();
    }

    private void use(MockHttpSession session) {
        SecurityContextHolder.setContext(context(session));
    }

    private SecurityContext context(MockHttpSession session) {
        return (SecurityContext) session.getAttribute(SPRING_SECURITY_CONTEXT_KEY);
    }

    private OrderRequest request(OrderItemRequest... items) {
        var request = new OrderRequest();
        request.setItems(List.of(items));
        return request;
    }

    private OrderItemRequest item(Long applianceId, Long quantity) {
        var item = new OrderItemRequest();
        item.setApplianceId(applianceId);
        item.setQuantity(quantity);
        return item;
    }
}