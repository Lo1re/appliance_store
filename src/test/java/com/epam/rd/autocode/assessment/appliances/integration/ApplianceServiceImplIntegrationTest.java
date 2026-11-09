package com.epam.rd.autocode.assessment.appliances.integration;

import com.epam.rd.autocode.assessment.appliances.dto.appliance.ApplianceRequest;
import com.epam.rd.autocode.assessment.appliances.exception.ApplianceException;
import com.epam.rd.autocode.assessment.appliances.model.*;
import com.epam.rd.autocode.assessment.appliances.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import java.math.BigDecimal;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:catalog-accounts-it",
    "store.demo.client-password=ClientTest2026!",
    "store.demo.employee-password=EmployeeTest2026!"
})
class ApplianceServiceImplIntegrationTest {
    private static final long MISSING = Long.MAX_VALUE;
    private static final PageRequest PAGE = PageRequest.of(0, 30, Sort.by("name"));
    @Autowired ManufacturerService manufacturers;
    @Autowired ApplianceService appliances;

    @Test
    void applianceCrudSearchAndForeignKeyChecks() {
        var samsung = manufacturers.findAll("Samsung", PAGE).getContent().get(0);
        var dell = manufacturers.findAll("Dell", PAGE).getContent().get(0);
        var created = appliances.create(appliance("  Integration Appliance  ", samsung.getId()));
        Long id = created.getId();
        assertThat(created.getManufacturerName()).isEqualTo("Samsung");
        assertThat(created.getManufacturerId()).isEqualTo(samsung.getId());
        assertThat(appliances.findById(id).getModel()).isEqualTo("TEST-MODEL");
        assertThat(appliances.findAll(null, PAGE)).extracting(a -> a.getId()).contains(id);
        assertThat(appliances.findAll(" TEST-model ", PAGE)).extracting(a -> a.getId()).contains(id);
        assertThat(appliances.findAll("Samsung", PAGE)).extracting(a -> a.getId()).contains(id);

        appliances.update(id, appliance("Revised appliance", dell.getId()));
        assertThat(appliances.findById(id).getManufacturerName()).isEqualTo("Dell");
        assertThat(appliances.findById(id).getName()).isEqualTo("Revised appliance");
        assertThatThrownBy(() -> appliances.create(appliance("Unknown brand", MISSING)))
                .isInstanceOf(ApplianceException.class).hasMessage("appliance.manufacturer.not-found");

        appliances.delete(id);
        assertThatThrownBy(() -> appliances.findById(id))
                .isInstanceOf(ApplianceException.class).hasMessage("appliance.not-found");
    }

    private ApplianceRequest appliance(String name, Long manufacturerId) {
        var request = new ApplianceRequest();
        request.setName(name);
        request.setCategory(Category.SMALL);
        request.setModel(" TEST-MODEL ");
        request.setManufacturerId(manufacturerId);
        request.setPowerType(PowerType.AC220);
        request.setCharacteristic(" Test characteristic ");
        request.setDescription(" Test description ");
        request.setPower(100);
        request.setPrice(new BigDecimal("10.00"));
        return request;
    }
}
