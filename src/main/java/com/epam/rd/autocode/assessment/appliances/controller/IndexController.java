package com.epam.rd.autocode.assessment.appliances.controller;

import com.epam.rd.autocode.assessment.appliances.service.ApplianceService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class IndexController {
    private final ApplianceService appliances;

    @GetMapping("/")
    public String home(HttpServletRequest request, Model model) {
        if (request.isUserInRole("CLIENT") || request.isUserInRole("EMPLOYEE")) {
            model.addAttribute("featured", appliances.findAll("", PageRequest.of(0, 4, Sort.by("name", "id"))).getContent());
        }
        return "index";
    }
}
