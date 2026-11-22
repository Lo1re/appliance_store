package com.epam.rd.autocode.assessment.appliances.web.pagination;

import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

/** Builds bounded, stable pagination without depending on MVC/API request DTOs. */
@Component
public class ListPageFactory {
    private final int defaultPageSize;

    public ListPageFactory(@Value("${store.pagination.page-size:10}") int defaultPageSize) {
        if (defaultPageSize < 1 || defaultPageSize > 100) {
            throw new IllegalArgumentException("store.pagination.page-size must be between 1 and 100");
        }
        this.defaultPageSize = defaultPageSize;
    }

    public ListPage create(int page, Integer requestedSize, String requestedSort,
                           Set<String> allowedSorts, String defaultSort) {
        int size = requestedSize == null ? defaultPageSize : requestedSize;
        if (size < 1 || size > 100) {
            throw new IllegalArgumentException("Page size must be between 1 and 100");
        }
        String sort = requestedSort != null && allowedSorts.contains(requestedSort) ? requestedSort : defaultSort;
        Sort ordering = Sort.by(sort);
        if (!"id".equals(sort)) ordering = ordering.and(Sort.by("id"));
        return new ListPage(sort, PageRequest.of(Math.max(0, page), size, ordering));
    }
}
