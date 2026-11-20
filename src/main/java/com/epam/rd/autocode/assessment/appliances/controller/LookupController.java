package com.epam.rd.autocode.assessment.appliances.controller;

import com.epam.rd.autocode.assessment.appliances.dto.lookup.SelectOption;
import com.epam.rd.autocode.assessment.appliances.service.LookupService;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Read-only, bounded data for the Thymeleaf form selectors. */
@RestController
@RequestMapping("/lookups")
@RequiredArgsConstructor
@Validated
public class LookupController {
    private final LookupService lookups;

    @GetMapping("/products")
    public List<SelectOption> products(@RequestParam(defaultValue = "") @Size(max = 100) String query) {
        return lookups.products(query, List.of());
    }
    @GetMapping("/clients")
    public List<SelectOption> clients(@RequestParam(defaultValue = "") @Size(max = 100) String query) {
        return lookups.clients(query, List.of());
    }
    @GetMapping("/manufacturers")
    public List<SelectOption> manufacturers(@RequestParam(defaultValue = "") @Size(max = 100) String query) {
        return lookups.manufacturers(query, List.of());
    }
}
