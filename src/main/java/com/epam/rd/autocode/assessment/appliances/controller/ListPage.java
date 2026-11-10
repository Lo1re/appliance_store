package com.epam.rd.autocode.assessment.appliances.controller;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.Set;

record ListPage(String sort, PageRequest pageable) {

    private static final int PAGE_SIZE = 10;

    static ListPage of(int page, String requestedSort, Set<String> allowedSorts) {
        String sort = allowedSorts.contains(requestedSort) ? requestedSort : "name";
        Sort ordering = Sort.by(sort);

        if (!"id".equals(sort)) {
            ordering = ordering.and(Sort.by("id"));
        }

        return new ListPage(sort, PageRequest.of(Math.max(0, page), PAGE_SIZE, ordering));
    }
}