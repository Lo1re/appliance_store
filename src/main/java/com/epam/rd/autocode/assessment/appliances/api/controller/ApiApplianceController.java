package com.epam.rd.autocode.assessment.appliances.api.controller;

import com.epam.rd.autocode.assessment.appliances.dto.appliance.ApplianceResponse;
import com.epam.rd.autocode.assessment.appliances.dto.query.ApiListQuery;
import com.epam.rd.autocode.assessment.appliances.dto.query.PageResponse;
import com.epam.rd.autocode.assessment.appliances.service.ApplianceService;
import com.epam.rd.autocode.assessment.appliances.web.pagination.ListPageFactory;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

@RestController
@RequestMapping(value = "/api/appliances", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@ConditionalOnProperty(name = "store.api.enabled", havingValue = "true")
public class ApiApplianceController {
    private static final Set<String> SORT_FIELDS = Set.of("id", "name", "price");
    private final ApplianceService service;
    private final ListPageFactory pages;

    @GetMapping
    public PageResponse<ApplianceResponse> list(@Valid @ModelAttribute ApiListQuery query) {
        var page =
                pages.create(
                        query.getPage(), query.getSize(), query.getSort(), SORT_FIELDS, "name");
        return PageResponse.from(service.findAll(query.getSearch(), page.pageable()));
    }

    @GetMapping("/{id}")
    public ApplianceResponse details(@PathVariable Long id) {
        return service.findById(id);
    }
}
