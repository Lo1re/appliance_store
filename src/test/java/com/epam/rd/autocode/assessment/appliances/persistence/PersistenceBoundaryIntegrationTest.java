package com.epam.rd.autocode.assessment.appliances.persistence;

import com.epam.rd.autocode.assessment.appliances.exception.AccountException;
import com.epam.rd.autocode.assessment.appliances.exception.CatalogException;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringJUnitConfig(PersistenceBoundaryIntegrationTest.Config.class)
class PersistenceBoundaryIntegrationTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired Writes writes;
    @Autowired PlatformTransactionManager transactions;

    @BeforeEach
    void prepare() {
        jdbc.execute("DROP TABLE IF EXISTS conflict_appliances");
        jdbc.execute("DROP TABLE IF EXISTS conflict_manufacturers");
        jdbc.execute("CREATE TABLE conflict_manufacturers (id INT PRIMARY KEY)");
        jdbc.execute("CREATE TABLE conflict_appliances (id INT PRIMARY KEY, manufacturer_id INT, CONSTRAINT fk_appliance_manufacturer FOREIGN KEY (manufacturer_id) REFERENCES conflict_manufacturers(id))");
        jdbc.update("INSERT INTO conflict_manufacturers VALUES (1)");
        jdbc.update("INSERT INTO conflict_appliances VALUES (1, 1)");
        jdbc.execute("DROP TABLE IF EXISTS conflict_users");
        jdbc.execute("CREATE TABLE conflict_users (id INT PRIMARY KEY, email VARCHAR(100), CONSTRAINT uq_users_email UNIQUE(email))");
        jdbc.update("INSERT INTO conflict_users VALUES (1, 'existing@example.com')");
    }

    @Test
    void translatesAfterRollbackAndLeavesTheDatabaseUsable() {
        assertThatThrownBy(writes::duplicate).isInstanceOf(AccountException.class);
        assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM conflict_users", Integer.class)).isEqualTo(1);
        jdbc.update("INSERT INTO conflict_users VALUES (4, 'after@example.com')");
    }

    @Test
    void preservesOriginalFailureWhenCallerOwnsTheTransaction() {
        assertThatThrownBy(() -> new TransactionTemplate(transactions).executeWithoutResult(status -> {
            try {
                writes.duplicate();
            } catch (RuntimeException failure) {
                assertThat(failure).isNotInstanceOf(AccountException.class);
                assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
                throw failure;
            }
        })).isNotInstanceOf(AccountException.class);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM conflict_users", Integer.class)).isEqualTo(1);
    }

    @Test
    void translatesForeignKeyFailureAndPreservesReferencedRows() {
        assertThatThrownBy(writes::deleteReferencedManufacturer)
                .isInstanceOf(CatalogException.class).hasMessage("manufacturer.in-use");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM conflict_manufacturers", Integer.class)).isEqualTo(1);
    }

    @Test
    void translatesFailureDuringCommitAsWellAsInsideMethod() {
        assertThatThrownBy(writes::duplicateDuringCommit).isInstanceOf(AccountException.class);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM conflict_users", Integer.class)).isEqualTo(1);
    }

    public static class Writes {
        private final JdbcTemplate jdbc;
        Writes(JdbcTemplate jdbc) { this.jdbc = jdbc; }

        @Transactional
        @PersistenceBoundary(PersistenceOperation.MANUFACTURER_DELETE)
        public void deleteReferencedManufacturer() {
            jdbc.update("DELETE FROM conflict_manufacturers WHERE id = 1");
        }

        @Transactional
        @PersistenceBoundary(PersistenceOperation.ACCOUNT_WRITE)
        public void duplicateDuringCommit() {
            jdbc.update("INSERT INTO conflict_users VALUES (2, 'first@example.com')");
            TransactionSynchronizationManager.registerSynchronization(
                    new org.springframework.transaction.support.TransactionSynchronization() {
                        @Override public void beforeCommit(boolean readOnly) {
                            jdbc.update("INSERT INTO conflict_users VALUES (3, 'existing@example.com')");
                        }
                    });
        }

        @Transactional
        @PersistenceBoundary(PersistenceOperation.ACCOUNT_WRITE)
        public void duplicate() {
            jdbc.update("INSERT INTO conflict_users VALUES (2, 'first@example.com')");
            jdbc.update("INSERT INTO conflict_users VALUES (3, 'existing@example.com')");
        }
    }

    @Configuration
    @EnableAspectJAutoProxy(proxyTargetClass = true)
    @EnableTransactionManagement
    static class Config {
        @Bean DataSource dataSource() {
            return new DriverManagerDataSource("jdbc:h2:mem:conflict-boundary;DB_CLOSE_DELAY=-1", "sa", "");
        }
        @Bean JdbcTemplate jdbc(DataSource source) { return new JdbcTemplate(source); }
        @Bean PlatformTransactionManager transactions(DataSource source) {
            return new DataSourceTransactionManager(source);
        }
        @Bean Writes writes(JdbcTemplate jdbc) { return new Writes(jdbc); }
        @Bean PersistenceConflictTranslator translator() { return new PersistenceConflictTranslator(new DatabaseConstraintExtractor()); }
        @Bean PersistenceConflictAspect aspect(PersistenceConflictTranslator translator) {
            return new PersistenceConflictAspect(translator);
        }
    }
}
