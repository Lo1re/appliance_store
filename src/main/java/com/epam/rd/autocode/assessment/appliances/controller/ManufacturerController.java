package com.epam.rd.autocode.assessment.appliances.controller;

import com.epam.rd.autocode.assessment.appliances.dto.manufacturer.ManufacturerRequest;
import com.epam.rd.autocode.assessment.appliances.exception.CatalogError;
import com.epam.rd.autocode.assessment.appliances.exception.CatalogException;
import com.epam.rd.autocode.assessment.appliances.mapper.ManufacturerMapper;
import com.epam.rd.autocode.assessment.appliances.service.ManufacturerService;
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
@ControllerSection(path = "/manufacturers", errorView = "error/catalog")
@RequiredArgsConstructor
@RequestMapping("/manufacturers")
public class ManufacturerController {

    private static final String LIST = "manufacture/manufacturers";
    private static final String FORM = "manufacture/newManufacturer";
    private static final String REDIRECT = "redirect:/manufacturers";
    private static final Set<String> SORT_FIELDS = Set.of("name", "id");

    private final ManufacturerService manufacturerService;
    private final ManufacturerMapper manufacturerMapper;

    private final ListPageFactory pages;

    @GetMapping
    public String list(
            @Valid @ModelAttribute ListQuery query,
            HttpServletRequest request,
            Model model
    ) {
        ListPage listPage = pages.create(query.getPage(), null, query.getSort(), SORT_FIELDS, "name");

        model.addAttribute("manufacturers", manufacturerService.findAll(query.getSearch(), listPage.pageable()));
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
        ManufacturerRequest request = id == null
                ? new ManufacturerRequest()
                : manufacturerMapper.toRequest(manufacturerService.findById(id));

        model.addAttribute("manufacturer", request);
        prepareForm(id, model);
        return FORM;
    }

    @PostMapping({"/add", "/{id}/edit"})
    public String save(
            @PathVariable(name = "id", required = false) Long id,
            @Valid @ModelAttribute("manufacturer") ManufacturerRequest request,
            BindingResult errors,
            Model model
    ) {
        prepareForm(id, model);

        if (errors.hasErrors()) {
            return FORM;
        }

        try {
            if (id == null) {
                manufacturerService.create(request);
            } else {
                manufacturerService.update(id, request);
            }

            return REDIRECT;
        } catch (CatalogException exception) {
            if (exception.error() != CatalogError.MANUFACTURER_DUPLICATE_NAME) {
                throw exception;
            }

            errors.rejectValue("name", exception.error().messageKey());
            return FORM;
        }
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        manufacturerService.delete(id);
        return REDIRECT;
    }

    private void prepareForm(Long id, Model model) {
        model.addAttribute("editing", id != null);
        model.addAttribute("formAction", id == null
                ? "/manufacturers/add"
                : "/manufacturers/" + id + "/edit");
    }
}
