package com.epam.rd.autocode.assessment.appliances.mapper;

import com.epam.rd.autocode.assessment.appliances.dto.manufacturer.ManufacturerRequest;
import com.epam.rd.autocode.assessment.appliances.dto.manufacturer.ManufacturerResponse;
import com.epam.rd.autocode.assessment.appliances.model.Manufacturer;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ManufacturerMapper {

    private final ModelMapper modelMapper;

    public void applyRequest(ManufacturerRequest request, Manufacturer manufacturer) {
        manufacturer.setName(request.getName());
    }

    public ManufacturerResponse toResponse(Manufacturer manufacturer) {
        return modelMapper.map(manufacturer, ManufacturerResponse.class);
    }
}