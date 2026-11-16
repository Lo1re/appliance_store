package com.epam.rd.autocode.assessment.appliances.integration;

import com.epam.rd.autocode.assessment.appliances.dto.manufacturer.ManufacturerRequest;
import com.epam.rd.autocode.assessment.appliances.logging.RequestLogContext;
import com.epam.rd.autocode.assessment.appliances.repository.ManufacturerRepository;
import com.epam.rd.autocode.assessment.appliances.repository.UserRepository;
import com.epam.rd.autocode.assessment.appliances.service.ManufacturerService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:business-audit-it;MODE=PostgreSQL;DB_CLOSE_DELAY=-1")
@WithUserDetails("phobos@gmail.com")
@ExtendWith(OutputCaptureExtension.class)
class BusinessAuditIntegrationTest {
    @Autowired ManufacturerService service;
    @Autowired ManufacturerRepository manufacturers;
    @Autowired UserRepository users;
    @Autowired PlatformTransactionManager transactions;

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void commitsOnlyAfterTheEnclosingTransactionAndKeepsCapturedIdentifiers(CapturedOutput output) {
        Long actor = users.findByEmailIgnoreCase("phobos@gmail.com").orElseThrow().getId();
        String requestId = "c1d8c3a3-8e54-4c09-867d-31570969d294";
        MDC.put(RequestLogContext.MDC_KEY, requestId);
        var saved = new TransactionTemplate(transactions).execute(status -> {
            var request = new ManufacturerRequest();
            request.setName("Audit committed manufacturer");
            var result = service.create(request);
            assertThat(output.getOut()).doesNotContain("Business change committed (action=MANUFACTURER_CREATED");
            assertThat(output.getOut()).doesNotContain("Service operation completed: ManufacturerServiceImpl.create");
            MDC.remove(RequestLogContext.MDC_KEY);
            return result;
        });
        try {
            assertThat(saved).isNotNull();
            assertThat(output.getOut()).contains("Business change committed (action=MANUFACTURER_CREATED",
                    "actorId=" + actor, "entityId=" + saved.getId(), "requestId=" + requestId);
        } finally {
            if (saved != null) manufacturers.deleteById(saved.getId());
        }
    }

    @Test
    void rollbackProducesNeitherBusinessSuccessNorTechnicalCompletion(CapturedOutput output) {
        new TransactionTemplate(transactions).executeWithoutResult(status -> {
            var request = new ManufacturerRequest();
            request.setName("Audit rolled back manufacturer");
            service.create(request);
            status.setRollbackOnly();
        });
        assertThat(manufacturers.existsByName("Audit rolled back manufacturer")).isFalse();
        assertThat(output.getOut()).doesNotContain("Business change committed (action=MANUFACTURER_CREATED",
                "Service operation completed: ManufacturerServiceImpl.create");
    }
}
