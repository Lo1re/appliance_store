package com.epam.rd.autocode.assessment.appliances.integration.postgresql;

import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.assertj.core.api.Assertions.*;

class PostgresMigrationContainerIT extends PostgresContainerSupport {
    @Autowired Flyway flyway;
    @Autowired JdbcTemplate jdbc;

    @Test void validatesAllMigrationsAndRestartPreservesUserChanges() {
        assertThat(jdbc.queryForObject("select version()", String.class)).startsWith("PostgreSQL 18");
        flyway.validate(); assertThat(flyway.info().pending()).isEmpty();
        assertThat(flyway.info().applied()).extracting(info -> info.getScript()).contains("V8__optional_unique_client_card.sql", "V9__pending_registration.sql");
        String email = "migration-" + UUID.randomUUID() + "@example.test";
        Long id = user(email); jdbc.update("update users set name = 'Preserved' where id = ?", id);
        assertThat(flyway.migrate().migrationsExecuted).isZero();
        assertThat(jdbc.queryForObject("select name from users where id = ?", String.class, id)).isEqualTo("Preserved");
    }
    @Test void postgresConstraintsAllowManyMissingCardsAndRejectDuplicateCardOrEmail() {
        String suffix = UUID.randomUUID().toString();
        Long a = user("card-a-" + suffix + "@example.test"); Long b = user("card-b-" + suffix + "@example.test");
        jdbc.update("insert into client(user_id,card) values (?, null), (?, null)", a, b);
        String card = suffix.substring(0, 30);
        jdbc.update("update client set card = ? where user_id = ?", card, a);
        assertThatThrownBy(() -> jdbc.update("update client set card = ? where user_id = ?", card, b)).isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> user("card-a-" + suffix + "@example.test")).isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("update client set card = '' where user_id = ?", b)).isInstanceOf(DataIntegrityViolationException.class);
    }
    private Long user(String email) {
        return jdbc.queryForObject("insert into users(name,email,password) values ('Container test',?,'not-a-login-credential') returning id", Long.class, email);
    }
}
