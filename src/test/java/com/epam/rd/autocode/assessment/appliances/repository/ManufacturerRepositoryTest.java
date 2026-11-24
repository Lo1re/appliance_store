package com.epam.rd.autocode.assessment.appliances.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.epam.rd.autocode.assessment.appliances.model.Manufacturer;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;

@org.springframework.context.annotation.Import(
        com.epam.rd.autocode.assessment.appliances.config.demo.DemoMigrationConfiguration.class)
@DataJpaTest
class ManufacturerRepositoryTest {
    @Autowired private ManufacturerRepository repository;

    @Test
    void findsExpectedSeedRecordIgnoringCase() {
        assertThat(
                        repository
                                .findByNameContainingIgnoreCase("sAmSuNg", PageRequest.of(0, 5))
                                .getContent())
                .extracting(Manufacturer::getName)
                .containsExactly("Samsung");
    }
}
