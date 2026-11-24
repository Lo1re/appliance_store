package com.epam.rd.autocode.assessment.appliances.integration;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

import javax.sql.DataSource;

@SpringBootTest(
        properties =
                "spring.datasource.url=jdbc:h2:mem:real-demo-catalog;MODE=PostgreSQL;DB_CLOSE_DELAY=-1")
@Transactional
class DemoCatalogIntegrationTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired DataSource dataSource;

    void replayCatalog() throws Exception {
        String source =
                new ClassPathResource("db/migration/demo/R__demo_data.sql")
                        .getContentAsString(StandardCharsets.UTF_8);
        String catalog =
                source.substring(source.indexOf("-- Upgrade only untouched legacy placeholders"));
        var script =
                new ResourceDatabasePopulator(
                        new ByteArrayResource(catalog.getBytes(StandardCharsets.UTF_8)));
        script.setSqlScriptEncoding("UTF-8");
        script.execute(dataSource);
    }

    @Test
    void realisticCatalogIsCompleteAndIdempotent() throws Exception {
        assertThat(jdbc.queryForObject("select count(*) from appliance", Integer.class))
                .isEqualTo(20);
        assertThat(
                        jdbc.queryForObject(
                                "select count(*) from appliance where model='-----' or"
                                        + " description='-----' or price < 100",
                                Integer.class))
                .isZero();
        assertThat(
                        jdbc.queryForObject(
                                "select count(*) from appliance where name like '%MacBook%'",
                                Integer.class))
                .isEqualTo(1);
        replayCatalog();
        replayCatalog();
        assertThat(jdbc.queryForObject("select count(*) from appliance", Integer.class))
                .isEqualTo(20);
        assertThat(jdbc.queryForObject("select count(*) from users", Integer.class)).isEqualTo(12);
    }

    @Test
    void legacyUpgradeKeepsOrderReferencesAndDoesNotOverwriteUserEdits() throws Exception {
        Long product =
                jdbc.queryForObject(
                        "select id from appliance where name='Samsung Galaxy S10'", Long.class);
        Long client =
                jdbc.queryForObject(
                        "select id from users where email='mercury@gmail.com'", Long.class);
        jdbc.update("insert into orders(client_id,approved) values (?,false)", client);
        Long order = jdbc.queryForObject("select max(id) from orders", Long.class);
        jdbc.update(
                "insert into order_row(appliance_id,orders_id,number,amount) values (?,?,1,4.04)",
                product,
                order);
        jdbc.update(
                "update appliance set"
                    + " name='Claw',model='-----',description='-----',price=1.01,category='BIG',power_type='ACCUMULATOR',characteristic='-------',power=600"
                    + " where id=?",
                product);
        jdbc.update(
                "update appliance set name='Edited by owner',price=12345 where model='MacBook Pro"
                        + " 14 M1 Pro'");
        replayCatalog();
        assertThat(
                        jdbc.queryForObject(
                                "select name from appliance where id=?", String.class, product))
                .isEqualTo("Samsung Galaxy S10");
        assertThat(
                        jdbc.queryForObject(
                                "select appliance_id from order_row where orders_id=?",
                                Long.class,
                                order))
                .isEqualTo(product);
        assertThat(
                        jdbc.queryForObject(
                                "select amount from order_row where orders_id=?",
                                BigDecimal.class,
                                order))
                .isEqualByComparingTo("4.04");
        assertThat(
                        jdbc.queryForObject(
                                "select name from appliance where model='MacBook Pro 14 M1 Pro'",
                                String.class))
                .isEqualTo("Edited by owner");
        assertThat(
                        jdbc.queryForObject(
                                "select price from appliance where model='MacBook Pro 14 M1 Pro'",
                                BigDecimal.class))
                .isEqualByComparingTo("12345");
        assertThat(jdbc.queryForObject("select count(*) from appliance", Integer.class))
                .isEqualTo(20);
    }

    @Test
    void aLegacyRowWithCustomSpecificationsIsPreserved() throws Exception {
        Long product =
                jdbc.queryForObject(
                        "select id from appliance where model='XPS 13 9300'", Long.class);
        jdbc.update(
                "update appliance set"
                    + " name='Ecu',model='-----',description='-----',price=3.01,category='SMALL',power_type='ACCUMULATOR',characteristic='Custom"
                    + " specification',power=800 where id=?",
                product);
        replayCatalog();
        assertThat(
                        jdbc.queryForObject(
                                "select characteristic from appliance where id=?",
                                String.class,
                                product))
                .isEqualTo("Custom specification");
        assertThat(
                        jdbc.queryForObject(
                                "select name from appliance where id=?", String.class, product))
                .isEqualTo("Ecu");
        assertThat(
                        jdbc.queryForObject(
                                "select count(*) from appliance where model='XPS 13 9300'",
                                Integer.class))
                .isEqualTo(1);
    }
}
