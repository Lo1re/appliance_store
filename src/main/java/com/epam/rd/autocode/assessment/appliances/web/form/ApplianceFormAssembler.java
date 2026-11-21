package com.epam.rd.autocode.assessment.appliances.web.form;

import com.epam.rd.autocode.assessment.appliances.dto.appliance.ApplianceRequest;
import com.epam.rd.autocode.assessment.appliances.mapper.ApplianceMapper;
import com.epam.rd.autocode.assessment.appliances.model.Category;
import com.epam.rd.autocode.assessment.appliances.model.PowerType;
import com.epam.rd.autocode.assessment.appliances.service.ApplianceService;
import com.epam.rd.autocode.assessment.appliances.service.lookup.LookupService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;

@Component
@RequiredArgsConstructor
public class ApplianceFormAssembler {
    private final ApplianceService appliances;
    private final ApplianceMapper mapper;
    private final LookupService lookups;

    public ApplianceRequest request(Long id) {
        return id == null ? new ApplianceRequest() : mapper.toRequest(appliances.findById(id));
    }

    public String render(Long id, ApplianceRequest request, Model model) {
        model.addAttribute("editing", id != null);
        model.addAttribute("formAction", id == null ? "/appliances/add" : "/appliances/" + id + "/edit");
        model.addAttribute("categories", Category.values());
        model.addAttribute("powerTypes", PowerType.values());
        model.addAttribute("manufacturers", lookups.manufacturers("", request.getManufacturerId() == null
                ? List.of() : List.of(request.getManufacturerId())));
        return "appliance/newAppliance";
    }
}
