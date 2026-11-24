package com.epam.rd.autocode.assessment.appliances.integration.postgresql;

import static org.assertj.core.api.Assertions.*;

import com.epam.rd.autocode.assessment.appliances.dto.client.ClientRequest;
import com.epam.rd.autocode.assessment.appliances.dto.user.PasswordRequest;
import com.epam.rd.autocode.assessment.appliances.exception.AccountError;
import com.epam.rd.autocode.assessment.appliances.exception.AccountException;
import com.epam.rd.autocode.assessment.appliances.repository.UserRepository;
import com.epam.rd.autocode.assessment.appliances.security.identity.StorePrincipal;
import com.epam.rd.autocode.assessment.appliances.service.ClientService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

class PostgresConcurrencyContainerIT extends PostgresContainerSupport {
    @Autowired ClientService clients;
    @Autowired UserRepository users;

    @Test
    void concurrentEmailCreatesOneClientAndReportsAFieldConflict() throws Exception {
        String email = "email-race-" + UUID.randomUUID() + "@example.test";
        assertRace(email, email, null, AccountError.EMAIL_DUPLICATE);
    }

    @Test
    void concurrentLoyaltyCardCreatesOneClientAndReportsAFieldConflict() throws Exception {
        String suffix = UUID.randomUUID().toString();
        assertRace(
                "card-a-" + suffix + "@example.test",
                "card-b-" + suffix + "@example.test",
                suffix.substring(0, 30),
                AccountError.CARD_DUPLICATE);
    }

    private void assertRace(String leftEmail, String rightEmail, String card, AccountError expected)
            throws Exception {
        var employee = users.findByEmailIgnoreCase("phobos@gmail.com").orElseThrow();
        var principal =
                new StorePrincipal(
                        employee.getId(), employee.getEmail(), employee.getPassword(), "EMPLOYEE");
        var start = new CyclicBarrier(2);
        var pool = Executors.newFixedThreadPool(2);
        try {
            java.util.function.Function<String, Callable<Object>> create =
                    email ->
                            () -> {
                                var context = SecurityContextHolder.createEmptyContext();
                                context.setAuthentication(
                                        UsernamePasswordAuthenticationToken.authenticated(
                                                principal, null, principal.getAuthorities()));
                                SecurityContextHolder.setContext(context);
                                try {
                                    var form = new ClientRequest();
                                    form.setName("Concurrent client");
                                    form.setEmail(email);
                                    form.setCard(card);
                                    var password = new PasswordRequest();
                                    password.setPassword("ContainerPassword2026!");
                                    start.await(5, TimeUnit.SECONDS);
                                    try {
                                        return clients.create(form, password).getId();
                                    } catch (AccountException conflict) {
                                        return conflict.error();
                                    }
                                } finally {
                                    SecurityContextHolder.clearContext();
                                }
                            };
            var a = pool.submit(create.apply(leftEmail));
            var b = pool.submit(create.apply(rightEmail));
            var results = List.of(a.get(30, TimeUnit.SECONDS), b.get(30, TimeUnit.SECONDS));
            assertThat(results).filteredOn(value -> value instanceof Long).hasSize(1);
            assertThat(results).filteredOn(expected::equals).hasSize(1);
        } finally {
            pool.shutdownNow();
        }
    }
}
