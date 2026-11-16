package com.epam.rd.autocode.assessment.appliances.integration;

import com.epam.rd.autocode.assessment.appliances.exception.AccountException;
import com.epam.rd.autocode.assessment.appliances.model.Employee;
import com.epam.rd.autocode.assessment.appliances.repository.EmployeeRepository;
import com.epam.rd.autocode.assessment.appliances.security.StorePrincipal;
import com.epam.rd.autocode.assessment.appliances.service.EmployeeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:employee-deletion-concurrency-it;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=5000")
class EmployeeDeletionConcurrencyIntegrationTest {
    @Autowired EmployeeService service;
    @Autowired EmployeeRepository employees;

    @Test
    void twoEmployeesCannotDeleteEachOtherAndLeaveTheStoreWithoutAnEmployee() throws Exception {
        var all = employees.findAll();
        Employee first = all.stream().filter(employee -> employee.getEmail().equals("phobos@gmail.com"))
                .findFirst().orElseThrow();
        Employee second = all.stream().filter(employee -> employee.getEmail().equals("moon@gmail.com"))
                .findFirst().orElseThrow();
        employees.deleteAll(all.stream().filter(employee -> !employee.getId().equals(first.getId())
                && !employee.getId().equals(second.getId())).toList());
        assertThat(employees.count()).isEqualTo(2);
        var ready = new CountDownLatch(2);
        var start = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(2);
        try {
            var left = executor.submit(deleteAs(first, second.getId(), ready, start));
            var right = executor.submit(deleteAs(second, first.getId(), ready, start));
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            var results = Arrays.asList(left.get(10, TimeUnit.SECONDS), right.get(10, TimeUnit.SECONDS));
            assertThat(results.stream().filter(Objects::isNull).count()).isEqualTo(1);
            assertThat(results.stream().filter(Objects::nonNull).findFirst().orElseThrow())
                    .isInstanceOfAny(AccountException.class, AccessDeniedException.class);
            assertThat(employees.count()).isEqualTo(1);
        } finally {
            start.countDown();
            executor.shutdownNow();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    private Callable<RuntimeException> deleteAs(Employee actor, Long target,
                                                  CountDownLatch ready, CountDownLatch start) {
        return () -> {
            var principal = new StorePrincipal(actor.getId(), actor.getEmail(), actor.getPassword(), "EMPLOYEE");
            var context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                    principal, null, principal.getAuthorities()));
            SecurityContextHolder.setContext(context);
            try {
                ready.countDown();
                if (!start.await(5, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("Concurrent test did not start");
                }
                try {
                    service.delete(target);
                    return null;
                } catch (RuntimeException failure) {
                    return failure;
                }
            } finally {
                SecurityContextHolder.clearContext();
            }
        };
    }
}
