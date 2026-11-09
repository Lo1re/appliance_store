package com.epam.rd.autocode.assessment.appliances.mapper;

import com.epam.rd.autocode.assessment.appliances.config.MainConfig;
import com.epam.rd.autocode.assessment.appliances.dto.manufacturer.ManufacturerRequest;
import com.epam.rd.autocode.assessment.appliances.mapper.ManufacturerMapper;
import com.epam.rd.autocode.assessment.appliances.model.*;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ManufacturerMapperTest {
    private final org.modelmapper.ModelMapper modelMapper = new MainConfig().modelMapper();

    @Test
    void updatesNameAndKeepsIdentity() {
        var manufacturer = new Manufacturer(5L, "Old");
        var manufacturerRequest = new ManufacturerRequest();
        manufacturerRequest.setName(" New Brand ");
        var manufacturerMapper = new ManufacturerMapper(modelMapper);
        manufacturerMapper.applyRequest(manufacturerRequest, manufacturer);
        var manufacturerResponse = manufacturerMapper.toResponse(manufacturer);
        assertThat(manufacturerResponse.getId()).isEqualTo(5L);
        assertThat(manufacturerResponse.getName()).isEqualTo("New Brand");
    }
}
