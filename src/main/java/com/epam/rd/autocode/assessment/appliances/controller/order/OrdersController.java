package com.epam.rd.autocode.assessment.appliances.controller.order;

import com.epam.rd.autocode.assessment.appliances.controller.advice.ControllerSection;
import com.epam.rd.autocode.assessment.appliances.dto.order.OrderRequest;
import com.epam.rd.autocode.assessment.appliances.dto.order.OrderResponse;
import com.epam.rd.autocode.assessment.appliances.dto.query.ListQuery;
import com.epam.rd.autocode.assessment.appliances.exception.OrderException;
import com.epam.rd.autocode.assessment.appliances.service.OrderService;
import com.epam.rd.autocode.assessment.appliances.web.OrderCapabilities;
import com.epam.rd.autocode.assessment.appliances.web.error.BusinessErrorStatus;
import com.epam.rd.autocode.assessment.appliances.web.form.OrderFormAssembler;
import com.epam.rd.autocode.assessment.appliances.web.form.OrderFormView;
import com.epam.rd.autocode.assessment.appliances.web.pagination.ListPage;
import com.epam.rd.autocode.assessment.appliances.web.pagination.ListPageFactory;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
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

import java.util.Set;

@Controller
@ControllerSection(path = "/orders", errorView = "error/general")
@RequiredArgsConstructor
@RequestMapping("/orders")
public class OrdersController {

    private static final String LIST = "order/orders";
    private static final String DETAILS = "order/details";
    private static final String REDIRECT = "redirect:/orders";
    private static final Set<String> SORT_FIELDS = Set.of("id", "approved");

    private final OrderService orderService;
    private final OrderFormAssembler orderFormAssembler;

    private final ListPageFactory pages;

    @GetMapping
    public String list(
            @Valid @ModelAttribute ListQuery query,
            @RequestParam(defaultValue = "all") String status,
            Model model) {
        ListPage listPage = pages.create(query.getPage(), null, query.getSort(), SORT_FIELDS, "id");
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
        return OrderFormView.render(orderFormAssembler.create(), model);
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        return OrderFormView.render(orderFormAssembler.edit(id), model);
    }

    @PostMapping({"/add", "/{id}/edit"})
    public String save(
            @PathVariable(name = "id", required = false) Long id,
            @RequestParam(name = "clientId", required = false) Long clientId,
            @Valid @ModelAttribute("orderForm") OrderRequest request,
            BindingResult errors,
            HttpServletResponse response,
            Model model,
            RedirectAttributes redirect) {
        if (errors.hasErrors()) {
            response.setStatus(HttpStatus.BAD_REQUEST.value());
            return OrderFormView.render(orderFormAssembler.redisplay(id, clientId, request), model);
        }

        try {
            var saved =
                    id == null
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
            response.setStatus(BusinessErrorStatus.of(exception).value());
            return OrderFormView.render(orderFormAssembler.redisplay(id, clientId, request), model);
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
}
