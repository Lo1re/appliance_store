package com.epam.rd.autocode.assessment.appliances.integration;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.UUID;

// Executes the actual V8 SQL without adding another Spring application context.
class ClientCardMigrationIntegrationTest {
    @Test
    void migratesExistingCardsAndDatabaseEnforcesOptionalUniqueness() throws Exception {
        try (var connection =
                        DriverManager.getConnection(
                                "jdbc:h2:mem:card-" + UUID.randomUUID() + ";MODE=PostgreSQL",
                                "sa",
                                "");
                var sql = connection.createStatement()) {
            sql.execute(
                    "CREATE TABLE client (user_id BIGINT PRIMARY KEY, card VARCHAR(32) NOT NULL)");
            sql.execute("INSERT INTO client VALUES (1, '  CARD-A  '), (2, ''), (3, '   ')");
            try (var input =
                    getClass()
                            .getResourceAsStream(
                                    "/db/migration/common/V8__optional_unique_client_card.sql")) {
                assertThat(input).isNotNull();
                for (String command :
                        new String(input.readAllBytes(), StandardCharsets.UTF_8).split(";")) {
                    if (!command.isBlank()) {
                        sql.execute(command);
                    }
                }
            }
            try (var result = sql.executeQuery("SELECT card FROM client WHERE user_id = 1")) {
                result.next();
                assertThat(result.getString(1)).isEqualTo("CARD-A");
            }
            try (var result = sql.executeQuery("SELECT count(*) FROM client WHERE card IS NULL")) {
                result.next();
                assertThat(result.getInt(1)).isEqualTo(2);
            }
            sql.execute("INSERT INTO client VALUES (4, NULL), (5, NULL)");
            assertThatThrownBy(() -> sql.execute("INSERT INTO client VALUES (6, 'CARD-A')"))
                    .isInstanceOf(SQLException.class);
            assertThatThrownBy(() -> sql.execute("INSERT INTO client VALUES (7, '')"))
                    .isInstanceOf(SQLException.class);
            assertThatThrownBy(() -> sql.execute("INSERT INTO client VALUES (8, ' CARD-B ')"))
                    .isInstanceOf(SQLException.class);
        }
    }
}
