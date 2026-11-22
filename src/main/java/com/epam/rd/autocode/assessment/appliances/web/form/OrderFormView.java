package com.epam.rd.autocode.assessment.appliances.web.form;

import org.springframework.ui.Model;

/** Maps prepared order-form data to the Thymeleaf model; it performs no database work. */
public final class OrderFormView {
    private OrderFormView() {
    }

    public static String render(OrderFormData form, Model model) {
        model.addAttribute("orderForm", form.request());
        model.addAttribute("editing", form.editing());
        model.addAttribute("formAction", form.editing()
                ? "/orders/" + form.orderId() + "/edit" : "/orders/add");
        model.addAttribute("cancelPath", form.editing() ? "/orders/" + form.orderId() : "/orders");
        model.addAttribute("selectClient", form.selectClient());
        model.addAttribute("selectedClientId", form.selectedClientId());
        model.addAttribute("products", form.products());
        model.addAttribute("clients", form.clients());
        return "order/form";
    }
}
