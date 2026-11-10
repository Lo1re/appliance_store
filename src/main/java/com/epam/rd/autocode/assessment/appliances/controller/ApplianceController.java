package com.epam.rd.autocode.assessment.appliances.controller;

import com.epam.rd.autocode.assessment.appliances.dto.appliance.ApplianceRequest;
import com.epam.rd.autocode.assessment.appliances.exception.CatalogError;
import com.epam.rd.autocode.assessment.appliances.exception.CatalogException;
import com.epam.rd.autocode.assessment.appliances.mapper.ApplianceMapper;
import com.epam.rd.autocode.assessment.appliances.model.Category;
import com.epam.rd.autocode.assessment.appliances.model.PowerType;
import com.epam.rd.autocode.assessment.appliances.service.ApplianceService;
import com.epam.rd.autocode.assessment.appliances.service.ManufacturerService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Set;

@Controller
@RequiredArgsConstructor
@RequestMapping("/appliances")
public class ApplianceController {

    private static final String LIST = "appliance/appliances";
    private static final String FORM = "appliance/newAppliance";
    private static final String REDIRECT = "redirect:/appliances";
    private static final Set<String> SORT_FIELDS = Set.of("name", "id", "price");

    private final ApplianceService applianceService;
    private final ApplianceMapper applianceMapper;
    private final ManufacturerService manufacturerService;

    @GetMapping
    public String list(
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "name") String sort,
            HttpServletRequest request,
            Model model
    ) {
        ListPage listPage = ListPage.of(page, sort, SORT_FIELDS);

        model.addAttribute("appliances", applianceService.findAll(search, listPage.pageable()));
        model.addAttribute("search", search);
        model.addAttribute("sort", listPage.sort());
        model.addAttribute("canManage", request.isUserInRole("EMPLOYEE"));
        return LIST;
    }

    @GetMapping({"/add", "/{id}/edit"})
    public String form(
            @PathVariable(name = "id", required = false) Long id,
            Model model
    ) {
        ApplianceRequest request = id == null
                ? new ApplianceRequest()
                : applianceMapper.toRequest(applianceService.findById(id));

        model.addAttribute("appliance", request);
        return renderForm(id, model);
    }

    @PostMapping({"/add", "/{id}/edit"})
    public String save(
            @PathVariable(name = "id", required = false) Long id,
            @Valid @ModelAttribute("appliance") ApplianceRequest request,
            BindingResult errors,
            Model model
    ) {
        if (errors.hasErrors()) {
            return renderForm(id, model);
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
            return renderForm(id, model);
        }
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        applianceService.delete(id);
        return REDIRECT;
    }

    private String renderForm(Long id, Model model) {
        model.addAttribute("editing", id != null);
        model.addAttribute("formAction", id == null
                ? "/appliances/add"
                : "/appliances/" + id + "/edit");
        model.addAttribute("categories", Category.values());
        model.addAttribute("powerTypes", PowerType.values());
        model.addAttribute("manufacturers", manufacturerService
                .findAll("", Pageable.unpaged(Sort.by("name")))
                .getContent());
        return FORM;
    }
}