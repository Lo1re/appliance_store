package com.epam.rd.autocode.assessment.appliances.mapper;

import com.epam.rd.autocode.assessment.appliances.dto.manufacturer.ManufacturerRequest;
import com.epam.rd.autocode.assessment.appliances.dto.manufacturer.ManufacturerResponse;
import com.epam.rd.autocode.assessment.appliances.model.Manufacturer;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(config = MappingConfig.class)
public interface ManufacturerMapper {
    @Mapping(target = "id", ignore = true)
    void applyRequest(ManufacturerRequest request, @MappingTarget Manufacturer entity);

    ManufacturerResponse toResponse(Manufacturer entity);

    ManufacturerRequest toRequest(ManufacturerResponse response);
}
