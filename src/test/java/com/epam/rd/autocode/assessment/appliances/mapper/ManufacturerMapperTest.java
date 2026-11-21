package com.epam.rd.autocode.assessment.appliances.mapper;

import com.epam.rd.autocode.assessment.appliances.dto.manufacturer.ManufacturerRequest;
import com.epam.rd.autocode.assessment.appliances.model.Manufacturer;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import static org.assertj.core.api.Assertions.assertThat;

class ManufacturerMapperTest {

    @Test
    void updatesNameAndKeepsIdentity() {
        var manufacturer = new Manufacturer(5L, "Old");
        var manufacturerRequest = new ManufacturerRequest();
        manufacturerRequest.setName(" New Brand ");
        var manufacturerMapper = Mappers.getMapper(ManufacturerMapper.class);
        manufacturerMapper.applyRequest(manufacturerRequest, manufacturer);
        var manufacturerResponse = manufacturerMapper.toResponse(manufacturer);
        assertThat(manufacturerResponse.getId()).isEqualTo(5L);
        assertThat(manufacturerResponse.getName()).isEqualTo("New Brand");
    }
}
