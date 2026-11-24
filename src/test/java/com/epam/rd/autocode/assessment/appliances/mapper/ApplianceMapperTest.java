package com.epam.rd.autocode.assessment.appliances.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.epam.rd.autocode.assessment.appliances.dto.appliance.ApplianceRequest;
import com.epam.rd.autocode.assessment.appliances.model.Appliance;
import com.epam.rd.autocode.assessment.appliances.model.Category;
import com.epam.rd.autocode.assessment.appliances.model.Manufacturer;
import com.epam.rd.autocode.assessment.appliances.model.PowerType;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;

class ApplianceMapperTest {

    @Test
    void keepsIdentityAndLeavesRelationshipForTheService() {
        var manufacturer = new Manufacturer(5L, "New Brand");
        var appliance = new Appliance();
        appliance.setId(11L);
        appliance.setManufacturer(manufacturer);
        var request = new ApplianceRequest();
        request.setName(" Product ");
        request.setModel(" M1 ");
        request.setManufacturerId(99L);
        request.setCategory(Category.SMALL);
        request.setPowerType(PowerType.AC220);
        request.setCharacteristic(" Compact ");
        request.setDescription(" Product description ");
        request.setPower(150);
        request.setPrice(new BigDecimal("12.30"));

        var mapper = Mappers.getMapper(ApplianceMapper.class);
        mapper.applyRequest(request, appliance);
        var response = mapper.toResponse(appliance);
        assertThat(response.getId()).isEqualTo(11L);
        assertThat(response.getName()).isEqualTo("Product");
        assertThat(response.getModel()).isEqualTo("M1");
        assertThat(response.getManufacturerId()).isEqualTo(5L);
        assertThat(response.getManufacturerName()).isEqualTo("New Brand");
        assertThat(response.getCategory()).isEqualTo(Category.SMALL);
        assertThat(response.getPowerType()).isEqualTo(PowerType.AC220);
        assertThat(response.getCharacteristic()).isEqualTo("Compact");
        assertThat(response.getDescription()).isEqualTo("Product description");
        assertThat(response.getPower()).isEqualTo(150);
        assertThat(response.getPrice()).isEqualByComparingTo("12.30");
    }
}
