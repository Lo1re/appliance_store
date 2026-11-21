package com.epam.rd.autocode.assessment.appliances.integration;

import com.epam.rd.autocode.assessment.appliances.model.User;
import com.epam.rd.autocode.assessment.appliances.repository.UserRepository;
import java.util.Map;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:flyway-initialization-it;MODE=PostgreSQL;DB_CLOSE_DELAY=-1")
class FlywayInitializationIntegrationTest {
    @Autowired Flyway flyway;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder encoder;
    @Autowired JdbcTemplate jdbc;

    @Test
    void migratesOnceAndPreservesDistinctCredentialHashesAndUserChanges() {
        Map<String, String> demoNames = Map.ofEntries(
                Map.entry("mercury@gmail.com", "Mercury"), Map.entry("venus@gmail.com", "Venus"),
                Map.entry("earth@gmail.com", "Earth"), Map.entry("iamgod@gmail.com", "Mars"),
                Map.entry("jupiter@gmail.com", "Jupiter"), Map.entry("saturn@gmail.com", "Saturn"),
                Map.entry("uranus@gmail.com", "Uranus"), Map.entry("neptune@gmail.com", "Neptune"),
                Map.entry("phobos@gmail.com", "Phobos"), Map.entry("moon@gmail.com", "Moon"),
                Map.entry("deimos@gmail.com", "Deimos"), Map.entry("europa@gmail.com", "Europa"));
        var accounts = users.findAll();
        assertThat(accounts).hasSize(12);
        assertThat(accounts).extracting(User::getPassword).doesNotHaveDuplicates();
        accounts.forEach(account -> {
            String password = demoNames.get(account.getEmail()) + "Local2026!";
            assertThat(account.getPassword()).startsWith("$2a$").isNotEqualTo(password);
            assertThat(encoder.matches(password, account.getPassword())).isTrue();
        });
        flyway.validate();
        assertThat(flyway.info().pending()).isEmpty();
        assertThat(flyway.info().applied())
                .extracting(migration -> migration.getScript())
                .containsExactlyInAnyOrder(
                        "V1__initial_schema.sql", "V2__login_attempt.sql", "V3__refresh_token.sql", "V4__password_reset_token.sql", "R__demo_data.sql");
        assertThat(jdbc.queryForObject("select count(*) from login_attempt", Long.class)).isZero();
        jdbc.update("update users set name = ? where email = ?", "Changed by user", "mercury@gmail.com");
        assertThat(flyway.migrate().migrationsExecuted).isZero();
        assertThat(users.findByEmailIgnoreCase("mercury@gmail.com").orElseThrow().getName())
                .isEqualTo("Changed by user");
        assertThat(users.count()).isEqualTo(12);
    }
}
