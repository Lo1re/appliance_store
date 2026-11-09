package com.epam.rd.autocode.assessment.appliances.mapper;

import com.epam.rd.autocode.assessment.appliances.dto.appliance.ApplianceRequest;
import com.epam.rd.autocode.assessment.appliances.dto.appliance.ApplianceResponse;
import com.epam.rd.autocode.assessment.appliances.model.Appliance;
import com.epam.rd.autocode.assessment.appliances.model.Manufacturer;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ApplianceMapper {

    private final ModelMapper modelMapper;

    public ApplianceResponse toResponse(Appliance appliance) {
        ApplianceResponse response = modelMapper.map(appliance, ApplianceResponse.class);

        Manufacturer manufacturer = appliance.getManufacturer();
        response.setManufacturerId(manufacturer.getId());
        response.setManufacturerName(manufacturer.getName());

        return response;
    }

    public void applyRequest(ApplianceRequest request, Appliance appliance) {
        appliance.setName(request.getName().strip());
        appliance.setCategory(request.getCategory());
        appliance.setModel(request.getModel().strip());
        appliance.setPowerType(request.getPowerType());
        appliance.setCharacteristic(request.getCharacteristic().strip());
        appliance.setDescription(request.getDescription().strip());
        appliance.setPower(request.getPower());
        appliance.setPrice(request.getPrice());
    }
}