package com.epam.rd.autocode.assessment.appliances.integration;

import com.epam.rd.autocode.assessment.appliances.dto.manufacturer.ManufacturerRequest;
import com.epam.rd.autocode.assessment.appliances.exception.CatalogException;
import com.epam.rd.autocode.assessment.appliances.service.ManufacturerService;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.test.context.support.WithUserDetails;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:catalog-accounts-it",
})
@WithUserDetails("phobos@gmail.com")
class ManufacturerServiceImplIntegrationTest {

    private static final PageRequest PAGE = PageRequest.of(0, 30, Sort.by("name"));

    @Autowired ManufacturerService manufacturers;

    @Test
    void manufacturerCrudSearchAndConflicts() {
        var samsung = manufacturers.findAll(" SAM ", PAGE).getContent().get(0);
        assertThat(samsung.getName()).isEqualTo("Samsung");
        assertThat(manufacturers.findAll(null, PageRequest.of(0, 2, Sort.by("name"))))
                .hasSize(2);
        assertThat(manufacturers.findAll(" ", PAGE)).isNotEmpty();

        var created = manufacturers.create(manufacturer("  Integration Brand  "));
        assertThat(manufacturers.findById(created.getId()).getName()).isEqualTo("Integration Brand");

        assertThatThrownBy(() -> manufacturers.create(manufacturer("Integration Brand")))
                .isInstanceOf(CatalogException.class).hasMessage("manufacturer.name.duplicate");
        assertThatThrownBy(() -> manufacturers.update(created.getId(), manufacturer("Samsung")))
                .isInstanceOf(CatalogException.class).hasMessage("manufacturer.name.duplicate");

        manufacturers.update(created.getId(), manufacturer("Integration Brand"));
        manufacturers.update(created.getId(), manufacturer("Renamed Brand"));
        assertThat(manufacturers.findById(created.getId()).getName()).isEqualTo("Renamed Brand");

        assertThatThrownBy(() -> manufacturers.delete(samsung.getId()))
                .isInstanceOf(CatalogException.class).hasMessage("manufacturer.in-use");

        manufacturers.delete(created.getId());
        assertThatThrownBy(() -> manufacturers.findById(created.getId()))
                .isInstanceOf(CatalogException.class).hasMessage("manufacturer.not-found");
        assertThatThrownBy(() -> manufacturers.create(manufacturer(" ")))
                .isInstanceOf(ConstraintViolationException.class);
    }

    private ManufacturerRequest manufacturer(String name) {
        var request = new ManufacturerRequest();
        request.setName(name);
        return request;
    }
}
