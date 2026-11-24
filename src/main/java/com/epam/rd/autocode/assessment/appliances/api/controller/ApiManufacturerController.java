package com.epam.rd.autocode.assessment.appliances.api.controller;

import com.epam.rd.autocode.assessment.appliances.dto.manufacturer.ManufacturerResponse;
import com.epam.rd.autocode.assessment.appliances.dto.query.ApiListQuery;
import com.epam.rd.autocode.assessment.appliances.dto.query.PageResponse;
import com.epam.rd.autocode.assessment.appliances.service.ManufacturerService;
import com.epam.rd.autocode.assessment.appliances.web.pagination.ListPageFactory;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

@RestController
@RequestMapping("/api/manufacturers")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "store.api.enabled", havingValue = "true")
public class ApiManufacturerController {
    private static final Set<String> SORT_FIELDS = Set.of("id", "name");
    private final ManufacturerService service;
    private final ListPageFactory pages;

    @GetMapping
    public PageResponse<ManufacturerResponse> list(@Valid @ModelAttribute ApiListQuery query) {
        var page =
                pages.create(
                        query.getPage(), query.getSize(), query.getSort(), SORT_FIELDS, "name");
        return PageResponse.from(service.findAll(query.getSearch(), page.pageable()));
    }

    @GetMapping("/{id}")
    public ManufacturerResponse details(@PathVariable Long id) {
        return service.findById(id);
    }
}
