package com.epam.rd.autocode.assessment.appliances.controller;

import com.epam.rd.autocode.assessment.appliances.dto.client.ClientRequest;
import com.epam.rd.autocode.assessment.appliances.dto.user.PasswordRequest;
import com.epam.rd.autocode.assessment.appliances.exception.AccountException;
import com.epam.rd.autocode.assessment.appliances.mapper.ClientMapper;
import com.epam.rd.autocode.assessment.appliances.service.ClientService;
import com.epam.rd.autocode.assessment.appliances.web.form.AccountFormView;
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

@Controller
@ControllerSection(path = "/clients", errorView = "error/account")
@RequiredArgsConstructor
@RequestMapping("/clients")
public class ClientController {

    private static final String BASE_PATH = "/clients";
    private static final String REDIRECT = "redirect:" + BASE_PATH;
    private static final Set<String> SORT_FIELDS = Set.of("id", "name", "email");

    private final ClientService clientService;
    private final ClientMapper clientMapper;
    private final AccountFormView accountFormView;

    @ModelAttribute
    public void addSectionAttributes(Model model) {
        model.addAttribute("basePath", BASE_PATH);
        model.addAttribute("section", "client");
    }

    private final ListPageFactory pages;

    @GetMapping
    public String list(
            @Valid @ModelAttribute ListQuery query,
            Model model
    ) {
        ListPage listPage = pages.create(query, SORT_FIELDS);
        model.addAttribute("accounts", clientService.findAll(query.getSearch(), listPage.pageable()));
        model.addAttribute("search", query.getSearch());
        model.addAttribute("sort", listPage.sort());
        return "account/list";
    }

    @GetMapping("/add")
    public String createForm(Model model) {
        model.addAttribute("account", new ClientRequest());
        model.addAttribute("credentials", new PasswordRequest());
        return renderForm(null, model);
    }

    @PostMapping("/add")
    public String create(
            @Valid @ModelAttribute("account") ClientRequest request,
            BindingResult accountErrors,
            @Valid @ModelAttribute("credentials") PasswordRequest credentials,
            BindingResult passwordErrors,
            Model model
    ) {
        if (accountErrors.hasErrors() || passwordErrors.hasErrors()) {
            return renderForm(null, model);
        }

        try {
            clientService.create(request, credentials);
            return REDIRECT;
        } catch (AccountException exception) {
            accountFormView.rejectDuplicateEmail(exception, accountErrors);
            return renderForm(null, model);
        }
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("account", clientMapper.toRequest(clientService.findById(id)));
        return renderForm(id, model);
    }

    @PostMapping("/{id}/edit")
    public String update(
            @PathVariable Long id,
            @Valid @ModelAttribute("account") ClientRequest request,
            BindingResult errors,
            Model model
    ) {
        if (errors.hasErrors()) {
            return renderForm(id, model);
        }

        try {
            clientService.update(id, request);
            return REDIRECT;
        } catch (AccountException exception) {
            accountFormView.rejectDuplicateEmail(exception, errors);
            return renderForm(id, model);
        }
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        clientService.delete(id);
        return REDIRECT;
    }

    private String renderForm(Long id, Model model) {
        return accountFormView.render(BASE_PATH, id, model);
    }
}
