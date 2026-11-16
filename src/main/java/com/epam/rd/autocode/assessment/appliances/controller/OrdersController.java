package com.epam.rd.autocode.assessment.appliances.controller;

import com.epam.rd.autocode.assessment.appliances.dto.order.OrderRequest;
import com.epam.rd.autocode.assessment.appliances.dto.order.OrderResponse;
import com.epam.rd.autocode.assessment.appliances.exception.OrderException;
import com.epam.rd.autocode.assessment.appliances.service.OrderService;
import com.epam.rd.autocode.assessment.appliances.web.OrderCapabilities;
import com.epam.rd.autocode.assessment.appliances.web.form.OrderFormAssembler;
import com.epam.rd.autocode.assessment.appliances.web.form.OrderFormData;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Set;
import lombok.RequiredArgsConstructor;
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

@Controller
@ControllerSection(path = "/orders", errorView = "error/general")
@RequiredArgsConstructor
@RequestMapping("/orders")
public class OrdersController {

    private static final String LIST = "order/orders";
    private static final String DETAILS = "order/details";
    private static final String FORM = "order/form";
    private static final String REDIRECT = "redirect:/orders";
    private static final Set<String> SORT_FIELDS = Set.of("id", "approved");

    private final OrderService orderService;
    private final OrderFormAssembler orderFormAssembler;

    @GetMapping
    public String list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "id") String sort,
            @RequestParam(defaultValue = "all") String status,
            Model model
    ) {
        ListPage listPage = ListPage.of(page, sort, SORT_FIELDS, "id");
        OrderFilter filter = OrderFilter.fromParameter(status);
        model.addAttribute("orders", orderService.findAll(filter.approved(), listPage.pageable()));
        model.addAttribute("sort", listPage.sort());
        model.addAttribute("status", filter.parameter());
        return LIST;
    }

    @GetMapping("/{id}")
    public String details(@PathVariable Long id, HttpServletRequest http, Model model) {
        OrderResponse order = orderService.findById(id);
        var capabilities = OrderCapabilities.forOrder(order, http.isUserInRole("EMPLOYEE"));

        model.addAttribute("order", order);
        model.addAttribute("canEdit", capabilities.canEdit());
        model.addAttribute("canDelete", capabilities.canDelete());
        model.addAttribute("canApprove", capabilities.canApprove());
        return DETAILS;
    }

    @GetMapping("/add")
    public String createForm(Model model) {
        return renderForm(orderFormAssembler.create(), model);
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        return renderForm(orderFormAssembler.edit(id), model);
    }

    @PostMapping({"/add", "/{id}/edit"})
    public String save(
            @PathVariable(name = "id", required = false) Long id,
            @RequestParam(name = "clientId", required = false) Long clientId,
            @Valid @ModelAttribute("orderForm") OrderRequest request,
            BindingResult errors,
            Model model,
            RedirectAttributes redirect
    ) {
        if (errors.hasErrors()) {
            return renderForm(orderFormAssembler.redisplay(
                    id, clientId, request), model);
        }

        try {
            var saved = id == null
                    ? orderService.create(clientId, request)
                    : orderService.update(id, request);
            redirect.addAttribute("orderId", saved.summary().id());
            return REDIRECT + "/{orderId}";
        } catch (OrderException exception) {
            switch (exception.error()) {
                case CLIENT_REQUIRED, CLIENT_NOT_FOUND, DUPLICATE_APPLIANCE, APPLIANCE_NOT_FOUND ->
                        errors.reject(exception.error().messageKey());
                default -> throw exception;
            }
            return renderForm(orderFormAssembler.redisplay(
                    id, clientId, request), model);
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

    private String renderForm(OrderFormData form, Model model) {
        model.addAttribute("orderForm", form.request());
        model.addAttribute("editing", form.editing());
        model.addAttribute("formAction", form.editing()
                ? "/orders/" + form.orderId() + "/edit" : "/orders/add");
        model.addAttribute("cancelPath", form.editing() ? "/orders/" + form.orderId() : "/orders");
        model.addAttribute("selectClient", form.selectClient());
        model.addAttribute("selectedClientId", form.selectedClientId());
        model.addAttribute("products", form.products());
        model.addAttribute("clients", form.clients());
        return FORM;
    }
}
