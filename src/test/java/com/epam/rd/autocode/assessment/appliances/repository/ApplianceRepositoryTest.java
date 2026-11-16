package com.epam.rd.autocode.assessment.appliances.repository;

import com.epam.rd.autocode.assessment.appliances.repository.ApplianceRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class ApplianceRepositoryTest {
    @Autowired EntityManager entityManager;
    @Autowired ApplianceRepository appliances;

    @Test
    void searchPaginatesAndFetchesManufacturer() {
        var page = PageRequest.of(0, 2, Sort.by("name"));
        var result = appliances.search("", page);
        assertThat(result).hasSize(2);
        assertThat(result.hasNext()).isTrue();
        assertThat(result.getContent()).extracting(a -> a.getName()).isSorted();

        var matches = appliances.search("sAmSuNg", page);
        assertThat(matches.getContent()).extracting(a -> a.getName()).contains("Claw");
        var util = entityManager.getEntityManagerFactory().getPersistenceUnitUtil();
        assertThat(matches.getContent()).allSatisfy(a -> assertThat(util.isLoaded(a, "manufacturer")).isTrue());
    }
}
