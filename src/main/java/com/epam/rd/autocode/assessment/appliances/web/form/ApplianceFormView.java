package com.epam.rd.autocode.assessment.appliances.web.form;

import com.epam.rd.autocode.assessment.appliances.model.Category;
import com.epam.rd.autocode.assessment.appliances.model.PowerType;

import org.springframework.ui.Model;

// Maps prepared appliance-form data to the model without loading persistent data.
public final class ApplianceFormView {
    private ApplianceFormView() {}

    public static String render(ApplianceFormData form, Model model) {
        model.addAttribute("appliance", form.request());
        model.addAttribute("editing", form.editing());
        model.addAttribute(
                "formAction",
                form.editing() ? "/appliances/" + form.id() + "/edit" : "/appliances/add");
        model.addAttribute("categories", Category.values());
        model.addAttribute("powerTypes", PowerType.values());
        model.addAttribute("manufacturers", form.manufacturers());
        return "appliance/newAppliance";
    }
}
