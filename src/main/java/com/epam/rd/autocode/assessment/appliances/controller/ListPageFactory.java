package com.epam.rd.autocode.assessment.appliances.controller;

import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

/** Builds bounded, stable pagination using the resource's allowed sort properties. */
@Component
public class ListPageFactory {
    private final int pageSize;

    public ListPageFactory(@Value("${store.pagination.page-size:10}") int pageSize) {
        if (pageSize < 1 || pageSize > 100) {
            throw new IllegalArgumentException("store.pagination.page-size must be between 1 and 100");
        }
        this.pageSize = pageSize;
    }

    ListPage create(ListQuery query, Set<String> allowedSorts) {
        return create(query, allowedSorts, "name");
    }

    ListPage create(ListQuery query, Set<String> allowedSorts, String defaultSort) {
        String requested = query.getSort();
        String sort = requested != null && allowedSorts.contains(requested) ? requested : defaultSort;
        Sort ordering = Sort.by(sort);
        if (!"id".equals(sort)) ordering = ordering.and(Sort.by("id"));
        return new ListPage(sort, PageRequest.of(Math.max(0, query.getPage()), pageSize, ordering));
    }
}
