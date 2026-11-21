package com.epam.rd.autocode.assessment.appliances.repository;

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
    @Autowired ManufacturerRepository manufacturers;

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
    @Test
    void customSearchTreatsPercentUnderscoreAndEscapeAsLiteralCharacters() {
        var products = appliances.findAll();
        var percent = products.get(0); percent.setName("Literal%mark");
        var underscore = products.get(1); underscore.setName("Literal_mark");
        var escape = products.get(2); escape.setName("Literal!mark");
        entityManager.flush();
        var page = PageRequest.of(0, 20);
        assertThat(appliances.search("%", page).getContent()).extracting(a -> a.getId()).containsExactly(percent.getId());
        assertThat(appliances.search("_", page).getContent()).extracting(a -> a.getId()).containsExactly(underscore.getId());
        assertThat(appliances.search("!", page).getContent()).extracting(a -> a.getId()).containsExactly(escape.getId());
    }

    @Test
    void derivedContainingQueryAlreadyEscapesWildcards() {
        var values = manufacturers.findAll();
        var literal = values.get(0); literal.setName("Literal%factory");
        values.get(1).setName("LiteralXfactory");
        entityManager.flush();
        assertThat(manufacturers.findByNameContainingIgnoreCase("%", PageRequest.of(0, 20)).getContent())
                .extracting(m -> m.getId()).containsExactly(literal.getId());
    }
}
