package com.epam.rd.autocode.assessment.appliances.web.form;

import com.epam.rd.autocode.assessment.appliances.exception.AccountException;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;

@Component
public class AccountFormView {
    public String render(String basePath, Long id, Model model) {
        model.addAttribute("editing", id != null);
        model.addAttribute("formAction", id == null ? basePath + "/add" : basePath + "/" + id + "/edit");
        return "account/form";
    }

    public void rejectDuplicate(AccountException exception, BindingResult errors) {
        String field = switch (exception.error()) {
            case EMAIL_DUPLICATE -> "email";
            case CARD_DUPLICATE -> "card";
            default -> throw exception;
        };
        errors.rejectValue(field, exception.error().messageKey());
    }
}
