package com.epam.rd.autocode.assessment.appliances.web.form;

import com.epam.rd.autocode.assessment.appliances.dto.appliance.ApplianceRequest;
import com.epam.rd.autocode.assessment.appliances.mapper.ApplianceMapper;
import com.epam.rd.autocode.assessment.appliances.service.ApplianceService;
import com.epam.rd.autocode.assessment.appliances.service.lookup.LookupService;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ApplianceFormAssembler {
    private final ApplianceService appliances;
    private final ApplianceMapper mapper;
    private final LookupService lookups;

    public ApplianceRequest request(Long id) {
        return id == null ? new ApplianceRequest() : mapper.toRequest(appliances.findById(id));
    }

    public ApplianceFormData prepare(Long id, ApplianceRequest request) {
        var selected =
                request.getManufacturerId() == null
                        ? List.<Long>of()
                        : List.of(request.getManufacturerId());
        return new ApplianceFormData(id, request, lookups.manufacturers("", selected));
    }
}
