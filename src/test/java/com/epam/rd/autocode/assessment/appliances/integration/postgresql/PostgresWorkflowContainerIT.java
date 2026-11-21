package com.epam.rd.autocode.assessment.appliances.integration.postgresql;

import com.epam.rd.autocode.assessment.appliances.dto.order.OrderRequest;
import com.epam.rd.autocode.assessment.appliances.dto.order.OrderItemRequest;
import com.epam.rd.autocode.assessment.appliances.exception.AccountException;
import com.epam.rd.autocode.assessment.appliances.exception.OrderException;
import com.epam.rd.autocode.assessment.appliances.exception.OrderError;
import com.epam.rd.autocode.assessment.appliances.model.Employee;
import com.epam.rd.autocode.assessment.appliances.model.User;
import com.epam.rd.autocode.assessment.appliances.repository.EmployeeRepository;
import com.epam.rd.autocode.assessment.appliances.repository.UserRepository;
import com.epam.rd.autocode.assessment.appliances.repository.ApplianceRepository;
import com.epam.rd.autocode.assessment.appliances.security.identity.StorePrincipal;
import com.epam.rd.autocode.assessment.appliances.service.EmployeeService;
import com.epam.rd.autocode.assessment.appliances.service.OrderService;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import static org.assertj.core.api.Assertions.*;

class PostgresWorkflowContainerIT extends PostgresContainerSupport {
    @Autowired OrderService orders;
    @Autowired EmployeeService employeeService;
    @Autowired EmployeeRepository employees;
    @Autowired UserRepository users;
    @Autowired ApplianceRepository appliances;

    @Test void twoApprovalsSerializeAndOnlyOneApprovesTheOrder() throws Exception {
        var employee = users.findByEmailIgnoreCase("phobos@gmail.com").orElseThrow();
        var item = new OrderItemRequest(); item.setApplianceId(appliances.findAll().get(0).getId()); item.setQuantity(1L);
        var form = new OrderRequest(); form.setItems(List.of(item));
        Long client = users.findByEmailIgnoreCase("mercury@gmail.com").orElseThrow().getId();
        Long id = asEmployee(employee, () -> orders.create(client, form).summary().id());
        var start = new CyclicBarrier(2); var pool = Executors.newFixedThreadPool(2);
        Callable<Object> approve = () -> asEmployee(employee, () -> {
            start.await(5, TimeUnit.SECONDS);
            try { return orders.approve(id).summary().approved(); }
            catch (OrderException conflict) { return conflict.error(); }
        });
        try {
            var a = pool.submit(approve); var b = pool.submit(approve);
            assertThat(List.of(a.get(30, TimeUnit.SECONDS), b.get(30, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(true, OrderError.ALREADY_APPROVED);
            assertThat(asEmployee(employee, () -> orders.findById(id).summary().approved())).isTrue();
        } finally { pool.shutdownNow(); }
    }
    @Test void mutualEmployeeDeletionCannotDeleteBothActors() throws Exception {
        String hash = users.findByEmailIgnoreCase("phobos@gmail.com").orElseThrow().getPassword();
        var a = employees.saveAndFlush(new Employee(null, "Left", "left-" + UUID.randomUUID() + "@example.test", hash, "Test"));
        var b = employees.saveAndFlush(new Employee(null, "Right", "right-" + UUID.randomUUID() + "@example.test", hash, "Test"));
        var start = new CyclicBarrier(2); var pool = Executors.newFixedThreadPool(2);
        java.util.function.BiFunction<Employee, Long, Callable<RuntimeException>> delete = (actor, target) -> () -> asEmployee(actor, () -> {
            start.await(5, TimeUnit.SECONDS);
            try { employeeService.delete(target); return null; }
            catch (AccountException | AccessDeniedException rejected) { return rejected; }
        });
        try {
            var left = pool.submit(delete.apply(a, b.getId())); var right = pool.submit(delete.apply(b, a.getId()));
            var outcomes = Arrays.asList(left.get(30, TimeUnit.SECONDS), right.get(30, TimeUnit.SECONDS));
            assertThat(outcomes.stream().filter(java.util.Objects::isNull).count()).isEqualTo(1);
            assertThat(employees.findAllById(List.of(a.getId(), b.getId()))).hasSize(1);
        } finally {
            pool.shutdownNow();
            employees.deleteAll(employees.findAllById(List.of(a.getId(), b.getId())));
        }
    }
    private <T> T asEmployee(User employee, Callable<T> action) throws Exception {
        var principal = new StorePrincipal(employee.getId(), employee.getEmail(), employee.getPassword(), "EMPLOYEE");
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities()));
        SecurityContextHolder.setContext(context);
        try { return action.call(); } finally { SecurityContextHolder.clearContext(); }
    }
}
