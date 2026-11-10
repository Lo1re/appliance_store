package com.epam.rd.autocode.assessment.appliances.controller;

import com.epam.rd.autocode.assessment.appliances.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Set;

@Controller
@RequiredArgsConstructor
@RequestMapping("/orders")
public class OrdersController {

    private static final Set<String> SORT_FIELDS = Set.of("id", "approved");
    private static final Set<String> STATUSES = Set.of("all", "pending", "approved");

    private final OrderService orderService;

    @GetMapping
    public String list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "id") String sort,
            @RequestParam(defaultValue = "all") String status,
            Model model
    ) {
        ListPage listPage = ListPage.of(page, sort, SORT_FIELDS, "id");
        String selectedStatus = STATUSES.contains(status) ? status : "all";
        Boolean approved = switch (selectedStatus) {
            case "approved" -> Boolean.TRUE;
            case "pending" -> Boolean.FALSE;
            default -> null;
        };

        model.addAttribute("orders", orderService.findAll(approved, listPage.pageable()));
        model.addAttribute("sort", listPage.sort());
        model.addAttribute("status", selectedStatus);
        return "order/orders";
    }

    @GetMapping("/{id}")
    public String details(@PathVariable Long id, Model model) {
        model.addAttribute("order", orderService.findById(id));
        return "order/details";
    }
}