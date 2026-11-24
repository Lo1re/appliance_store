package com.epam.rd.autocode.assessment.appliances.integration;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.epam.rd.autocode.assessment.appliances.dto.client.ClientRequest;
import com.epam.rd.autocode.assessment.appliances.dto.manufacturer.ManufacturerRequest;
import com.epam.rd.autocode.assessment.appliances.dto.user.PasswordRequest;
import com.epam.rd.autocode.assessment.appliances.exception.AccountError;
import com.epam.rd.autocode.assessment.appliances.exception.BusinessException;
import com.epam.rd.autocode.assessment.appliances.exception.CatalogError;
import com.epam.rd.autocode.assessment.appliances.exception.CatalogException;
import com.epam.rd.autocode.assessment.appliances.model.Appliance;
import com.epam.rd.autocode.assessment.appliances.model.Category;
import com.epam.rd.autocode.assessment.appliances.model.Manufacturer;
import com.epam.rd.autocode.assessment.appliances.model.PowerType;
import com.epam.rd.autocode.assessment.appliances.repository.ApplianceRepository;
import com.epam.rd.autocode.assessment.appliances.repository.ManufacturerRepository;
import com.epam.rd.autocode.assessment.appliances.repository.UserRepository;
import com.epam.rd.autocode.assessment.appliances.service.ClientService;
import com.epam.rd.autocode.assessment.appliances.service.ManufacturerService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@SpringBootTest(
        properties =
                "spring.datasource.url=jdbc:h2:mem:foundation-races;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000")
@ActiveProfiles("test")
@WithUserDetails("phobos@gmail.com")
class PersistenceRaceIntegrationTest {
    @Autowired ClientService clients;
    @Autowired ManufacturerService manufacturers;
    @Autowired JdbcTemplate jdbc;
    @Autowired PlatformTransactionManager transactions;
    @MockitoSpyBean UserRepository users;
    @MockitoSpyBean ManufacturerRepository manufacturerRepository;
    @MockitoSpyBean ApplianceRepository appliances;

    @Test
    void duplicateEmailIsTranslatedWhenBothRequestsPassThePrecheck() throws Exception {
        var barrier = new CyclicBarrier(2);
        doAnswer(
                        invocation -> {
                            barrier.await(10, TimeUnit.SECONDS);
                            return false;
                        })
                .when(users)
                .existsByEmailIgnoreCase("foundation-race@example.com");
        var outcomes =
                race(
                        () -> {
                            var request = new ClientRequest();
                            request.setName("Race client");
                            request.setEmail("foundation-race@example.com");
                            request.setCard("RACE-CARD");
                            var password = new PasswordRequest();
                            password.setPassword("FoundationRace2026!");
                            return clients.create(request, password).getId();
                        });
        assertThat(outcomes).filteredOn(AccountError.EMAIL_DUPLICATE::equals).hasSize(1);
        assertThat(outcomes).filteredOn(value -> value instanceof Long).hasSize(1);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT COUNT(*) FROM users WHERE email = ?",
                                Integer.class,
                                "foundation-race@example.com"))
                .isEqualTo(1);
    }

    @Test
    void duplicateNameIsTranslatedWhenBothRequestsPassThePrecheck() throws Exception {
        var barrier = new CyclicBarrier(2);
        doAnswer(
                        invocation -> {
                            barrier.await(10, TimeUnit.SECONDS);
                            return false;
                        })
                .when(manufacturerRepository)
                .existsByName("Foundation racing maker");
        var outcomes =
                race(
                        () -> {
                            var request = new ManufacturerRequest();
                            request.setName("Foundation racing maker");
                            return manufacturers.create(request).getId();
                        });
        assertThat(outcomes)
                .filteredOn(CatalogError.MANUFACTURER_DUPLICATE_NAME::equals)
                .hasSize(1);
        assertThat(outcomes).filteredOn(value -> value instanceof Long).hasSize(1);
    }

    @Test
    void newlyCommittedReferencePreventsDeletionAfterThePrecheck() {
        var parent =
                manufacturerRepository.saveAndFlush(new Manufacturer(null, "Foundation FK maker"));
        var concurrent = new TransactionTemplate(transactions);
        concurrent.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        doAnswer(
                        invocation -> {
                            concurrent.executeWithoutResult(
                                    status -> {
                                        var appliance = new Appliance();
                                        appliance.setName("Concurrent FK appliance");
                                        appliance.setModel("FK-1");
                                        appliance.setManufacturer(
                                                manufacturerRepository.getReferenceById(
                                                        parent.getId()));
                                        appliance.setCategory(Category.SMALL);
                                        appliance.setPowerType(PowerType.AC220);
                                        appliance.setCharacteristic("Test");
                                        appliance.setDescription("Test");
                                        appliance.setPower(100);
                                        appliance.setPrice(new BigDecimal("1.00"));
                                        appliances.saveAndFlush(appliance);
                                    });
                            return false;
                        })
                .when(appliances)
                .existsByManufacturer_Id(parent.getId());
        assertThatThrownBy(() -> manufacturers.delete(parent.getId()))
                .isInstanceOf(CatalogException.class)
                .hasMessage("manufacturer.in-use");
        assertThat(manufacturerRepository.existsById(parent.getId())).isTrue();
        assertThat(
                        jdbc.queryForObject(
                                "SELECT COUNT(*) FROM appliance WHERE manufacturer_id = ?",
                                Integer.class,
                                parent.getId()))
                .isEqualTo(1);
    }

    private List<Object> race(Callable<Long> operation) throws Exception {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        var executor = Executors.newFixedThreadPool(2);
        Callable<Object> attempt =
                () -> {
                    var context = SecurityContextHolder.createEmptyContext();
                    context.setAuthentication(authentication);
                    SecurityContextHolder.setContext(context);
                    try {
                        return operation.call();
                    } catch (BusinessException expected) {
                        return expected.error();
                    } finally {
                        SecurityContextHolder.clearContext();
                    }
                };
        try {
            var first = executor.submit(attempt);
            var second = executor.submit(attempt);
            return List.of(first.get(20, TimeUnit.SECONDS), second.get(20, TimeUnit.SECONDS));
        } finally {
            executor.shutdownNow();
        }
    }
}
