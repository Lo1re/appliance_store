package com.epam.rd.autocode.assessment.appliances.controller;

import java.util.Set;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

record ListPage(String sort, PageRequest pageable) {

    private static final int PAGE_SIZE = 10;

    static ListPage of(int page, String requestedSort, Set<String> allowedSorts) {
        return of(page, requestedSort, allowedSorts, "name");
    }

    static ListPage of(
            int page,
            String requestedSort,
            Set<String> allowedSorts,
            String defaultSort
    ) {
        String sort = allowedSorts.contains(requestedSort) ? requestedSort : defaultSort;
        Sort ordering = Sort.by(sort);

        if (!"id".equals(sort)) {
            ordering = ordering.and(Sort.by("id"));
        }

        return new ListPage(sort, PageRequest.of(Math.max(0, page), PAGE_SIZE, ordering));
    }
}
