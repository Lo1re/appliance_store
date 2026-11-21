package com.epam.rd.autocode.assessment.appliances.repository;

import com.epam.rd.autocode.assessment.appliances.model.Client;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class ClientRepositoryTest {
    @Autowired private ClientRepository repository;

    @Test
    void findsExpectedSeedRecordIgnoringCase() {
        assertThat(repository.findByNameContainingIgnoreCaseOrEmailContainingIgnoreCase("not a match", "MERCURY", PageRequest.of(0, 5)).getContent())
                .extracting(Client::getName)
                .containsExactly("Mercury");
    }
}
