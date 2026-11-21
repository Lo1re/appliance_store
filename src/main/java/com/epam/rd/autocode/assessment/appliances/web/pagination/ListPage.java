package com.epam.rd.autocode.assessment.appliances.web.pagination;

import org.springframework.data.domain.PageRequest;

/** Resolved transport pagination, shared by MVC and JSON endpoints. */
public record ListPage(String sort, PageRequest pageable) { }
