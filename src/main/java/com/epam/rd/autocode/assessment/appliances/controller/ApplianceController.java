package com.epam.rd.autocode.assessment.appliances.controller;

import com.epam.rd.autocode.assessment.appliances.dto.appliance.ApplianceRequest;
import com.epam.rd.autocode.assessment.appliances.exception.CatalogError;
import com.epam.rd.autocode.assessment.appliances.exception.CatalogException;
import com.epam.rd.autocode.assessment.appliances.service.ApplianceService;
import com.epam.rd.autocode.assessment.appliances.web.form.ApplianceFormAssembler;
import com.epam.rd.autocode.assessment.appliances.web.pagination.ListPage;
import com.epam.rd.autocode.assessment.appliances.web.pagination.ListPageFactory;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@ControllerSection(path = "/appliances", errorView = "error/catalog")
@RequiredArgsConstructor
@RequestMapping("/appliances")
public class ApplianceController {

    private static final String LIST = "appliance/appliances";
    private static final String REDIRECT = "redirect:/appliances";
    private static final Set<String> SORT_FIELDS = Set.of("name", "id", "price");

    private final ApplianceService applianceService;
    private final ApplianceFormAssembler forms;

    private final ListPageFactory pages;

    @GetMapping
    public String list(
            @Valid @ModelAttribute ListQuery query,
            HttpServletRequest request,
            Model model
    ) {
        ListPage listPage = pages.create(query.getPage(), null, query.getSort(), SORT_FIELDS, "name");

        model.addAttribute("appliances", applianceService.findAll(query.getSearch(), listPage.pageable()));
        model.addAttribute("search", query.getSearch());
        model.addAttribute("sort", listPage.sort());
        model.addAttribute("canManage", request.isUserInRole("EMPLOYEE"));
        return LIST;
    }

    @GetMapping({"/add", "/{id}/edit"})
    public String form(
            @PathVariable(name = "id", required = false) Long id,
            Model model
    ) {
        ApplianceRequest request = forms.request(id);
        model.addAttribute("appliance", request);
        return forms.render(id, request, model);
    }

    @PostMapping({"/add", "/{id}/edit"})
    public String save(
            @PathVariable(name = "id", required = false) Long id,
            @Valid @ModelAttribute("appliance") ApplianceRequest request,
            BindingResult errors,
            Model model
    ) {
        if (errors.hasErrors()) {
            return forms.render(id, request, model);
        }

        try {
            if (id == null) {
                applianceService.create(request);
            } else {
                applianceService.update(id, request);
            }

            return REDIRECT;
        } catch (CatalogException exception) {
            if (exception.error() != CatalogError.APPLIANCE_MANUFACTURER_NOT_FOUND) {
                throw exception;
            }

            errors.rejectValue("manufacturerId", exception.error().messageKey());
            return forms.render(id, request, model);
        }
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        applianceService.delete(id);
        return REDIRECT;
    }

}
