package com.epam.rd.autocode.assessment.appliances.integration.postgresql;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

// One disposable PostgreSQL per test JVM; Testcontainers' resource reaper owns cleanup.
@ActiveProfiles("test")
@SpringBootTest(
        properties = {
            "spring.docker.compose.enabled=false",
            "store.security.web-auth=jwt",
            "store.security.google.enabled=false",
            "store.security.jwt.secret-base64=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="
        })
abstract class PostgresContainerSupport {
    private static final class Database {
        static final PostgreSQLContainer<?> INSTANCE = start();

        private static PostgreSQLContainer<?> start() {
            var database =
                    new PostgreSQLContainer<>("postgres:18.1-alpine")
                            .withDatabaseName("appliance_store_it")
                            .withUsername("store_test")
                            .withPassword("container-test-only")
                            .withCommand(
                                    "postgres",
                                    "-c",
                                    "lock_timeout=10000",
                                    "-c",
                                    "statement_timeout=20000")
                            .withReuse(false);
            database.start(); // Fail explicitly if Docker is unavailable. Never fall back to
            // H2/local DB.
            return database;
        }
    }

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry properties) {
        var db = Database.INSTANCE;
        properties.add("spring.datasource.url", db::getJdbcUrl);
        properties.add("spring.datasource.username", db::getUsername);
        properties.add("spring.datasource.password", db::getPassword);
        properties.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    }
}
