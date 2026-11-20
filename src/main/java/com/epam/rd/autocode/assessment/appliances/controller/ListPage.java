package com.epam.rd.autocode.assessment.appliances.controller;

import org.springframework.data.domain.PageRequest;

record ListPage(String sort, PageRequest pageable) { }
