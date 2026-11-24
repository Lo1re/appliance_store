package com.epam.rd.autocode.assessment.appliances.web.form;

import com.epam.rd.autocode.assessment.appliances.dto.appliance.ApplianceRequest;
import com.epam.rd.autocode.assessment.appliances.dto.lookup.SelectOption;

import java.util.List;

public record ApplianceFormData(
        Long id, ApplianceRequest request, List<SelectOption> manufacturers) {
    public ApplianceFormData {
        manufacturers = List.copyOf(manufacturers);
    }

    public boolean editing() {
        return id != null;
    }
}
