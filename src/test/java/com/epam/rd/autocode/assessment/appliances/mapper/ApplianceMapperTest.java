package com.epam.rd.autocode.assessment.appliances.mapper;

import com.epam.rd.autocode.assessment.appliances.config.MainConfig;
import com.epam.rd.autocode.assessment.appliances.dto.appliance.ApplianceRequest;
import com.epam.rd.autocode.assessment.appliances.mapper.ApplianceMapper;
import com.epam.rd.autocode.assessment.appliances.model.*;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.assertj.core.api.Assertions.assertThat;

class ApplianceMapperTest {
    private final org.modelmapper.ModelMapper modelMapper = new MainConfig().modelMapper();

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

        var mapper = new ApplianceMapper(modelMapper);
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
