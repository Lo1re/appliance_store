package com.epam.rd.autocode.assessment.appliances.mapper;

import com.epam.rd.autocode.assessment.appliances.dto.appliance.ApplianceRequest;
import com.epam.rd.autocode.assessment.appliances.dto.appliance.ApplianceResponse;
import com.epam.rd.autocode.assessment.appliances.model.Appliance;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(config = MappingConfig.class)
public interface ApplianceMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "manufacturer", ignore = true)
    void applyRequest(ApplianceRequest request, @MappingTarget Appliance entity);

    @Mapping(target = "manufacturerId", source = "manufacturer.id")
    @Mapping(target = "manufacturerName", source = "manufacturer.name")
    ApplianceResponse toResponse(Appliance entity);

    ApplianceRequest toRequest(ApplianceResponse response);
}
