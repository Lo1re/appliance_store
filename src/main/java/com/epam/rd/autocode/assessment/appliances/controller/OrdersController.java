package com.epam.rd.autocode.assessment.appliances.controller;

import com.epam.rd.autocode.assessment.appliances.dto.order.OrderItemRequest;
import com.epam.rd.autocode.assessment.appliances.dto.order.OrderRequest;
import com.epam.rd.autocode.assessment.appliances.dto.order.OrderResponse;
import com.epam.rd.autocode.assessment.appliances.exception.OrderException;
import com.epam.rd.autocode.assessment.appliances.mapper.OrderMapper;
import com.epam.rd.autocode.assessment.appliances.service.ApplianceService;
import com.epam.rd.autocode.assessment.appliances.service.ClientService;
import com.epam.rd.autocode.assessment.appliances.service.OrderService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Controller
@RequiredArgsConstructor
@RequestMapping("/orders")
public class OrdersController {

    private static final String LIST = "order/orders";
    private static final String DETAILS = "order/details";
    private static final String FORM = "order/form";
    private static final String REDIRECT = "redirect:/orders";
    private static final Set<String> SORT_FIELDS = Set.of("id", "approved");
    private static final Set<String> STATUSES = Set.of("all", "pending", "approved");

    private static final Set<String> FORM_ERROR_KEYS = Set.of(
            "order.client.required",
            "order.client.not-found",
            "order.appliance.duplicate",
            "order.appliance.not-found"
    );

    private final OrderService orderService;
    private final OrderMapper orderMapper;
    private final ApplianceService applianceService;
    private final ClientService clientService;

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
        return LIST;
    }

    @GetMapping("/{id}")
    public String details(@PathVariable Long id, HttpServletRequest http, Model model) {
        OrderResponse order = orderService.findById(id);
        boolean editable = !order.summary().approved();

        model.addAttribute("order", order);
        model.addAttribute("canEdit", editable);
        model.addAttribute("canDelete", editable);
        model.addAttribute("canApprove", http.isUserInRole("EMPLOYEE") && editable);
        return DETAILS;
    }

    @GetMapping("/add")
    public String createForm(HttpServletRequest http, Model model) {
        var request = new OrderRequest();
        model.addAttribute("orderForm", request);
        return renderForm(request, null, null, http, model);
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, HttpServletRequest http, Model model) {
        OrderRequest request = orderMapper.toRequest(requireEditableOrder(id));
        model.addAttribute("orderForm", request);
        return renderForm(request, id, null, http, model);
    }

    @PostMapping({"/add", "/{id}/edit"})
    public String save(
            @PathVariable(name = "id", required = false) Long id,
            @RequestParam(name = "clientId", required = false) Long clientId,
            @Valid @ModelAttribute("orderForm") OrderRequest request,
            BindingResult errors,
            HttpServletRequest http,
            Model model,
            RedirectAttributes redirect
    ) {
        if (id != null) {
            requireEditableOrder(id);
        }

        if (errors.hasErrors()) {
            return renderForm(request, id, clientId, http, model);
        }

        try {
            var saved = id == null
                    ? orderService.create(clientId, request)
                    : orderService.update(id, request);
            redirect.addAttribute("orderId", saved.summary().id());
            return REDIRECT + "/{orderId}";
        } catch (OrderException exception) {
            if (!FORM_ERROR_KEYS.contains(exception.getMessage())) {
                throw exception;
            }

            errors.reject(exception.getMessage());
            return renderForm(request, id, clientId, http, model);
        }
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        orderService.delete(id);
        return REDIRECT;
    }

    @PostMapping("/{id}/approve")
    public String approve(@PathVariable Long id, RedirectAttributes redirect) {
        orderService.approve(id);
        redirect.addAttribute("orderId", id);
        return REDIRECT + "/{orderId}";
    }

    private OrderResponse requireEditableOrder(Long id) {
        OrderResponse order = orderService.findById(id);

        if (order.summary().approved()) {
            throw new OrderException("order.not-editable");
        }

        return order;
    }

    private String renderForm(
            OrderRequest request,
            Long id,
            Long clientId,
            HttpServletRequest http,
            Model model
    ) {
        if (request.getItems() == null || request.getItems().isEmpty()) {
            request.setItems(new ArrayList<>(List.of(new OrderItemRequest())));
        }

        boolean editing = id != null;
        boolean selectClient = !editing && http.isUserInRole("EMPLOYEE");
        Pageable options = Pageable.unpaged(Sort.by("name", "id"));

        model.addAttribute("editing", editing);
        model.addAttribute("formAction", editing ? "/orders/" + id + "/edit" : "/orders/add");
        model.addAttribute("cancelPath", editing ? "/orders/" + id : "/orders");
        model.addAttribute("selectClient", selectClient);
        model.addAttribute("selectedClientId", clientId);
        model.addAttribute("products", applianceService.findAll("", options).getContent());

        if (selectClient) {
            model.addAttribute("clients", clientService.findAll("", options).getContent());
        }

        return FORM;
    }
}