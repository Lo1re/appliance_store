package com.epam.rd.autocode.assessment.appliances.controller.account;

import com.epam.rd.autocode.assessment.appliances.controller.advice.ControllerSection;
import com.epam.rd.autocode.assessment.appliances.dto.employee.EmployeeRequest;
import com.epam.rd.autocode.assessment.appliances.dto.query.ListQuery;
import com.epam.rd.autocode.assessment.appliances.dto.user.PasswordRequest;
import com.epam.rd.autocode.assessment.appliances.exception.AccountException;
import com.epam.rd.autocode.assessment.appliances.mapper.EmployeeMapper;
import com.epam.rd.autocode.assessment.appliances.service.EmployeeService;
import com.epam.rd.autocode.assessment.appliances.web.error.BusinessErrorStatus;
import com.epam.rd.autocode.assessment.appliances.web.form.AccountFormView;
import com.epam.rd.autocode.assessment.appliances.web.pagination.ListPage;
import com.epam.rd.autocode.assessment.appliances.web.pagination.ListPageFactory;

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

import java.util.Set;

@Controller
@ControllerSection(path = "/employees", errorView = "error/account")
@RequiredArgsConstructor
@RequestMapping("/employees")
public class EmployeeController {

    private static final String BASE_PATH = "/employees";
    private static final String REDIRECT = "redirect:" + BASE_PATH;
    private static final Set<String> SORT_FIELDS = Set.of("id", "name", "email");

    private final EmployeeService employeeService;
    private final EmployeeMapper employeeMapper;
    private final AccountFormView accountFormView;

    private final ListPageFactory pages;

    @ModelAttribute
    public void addSectionAttributes(Model model) {
        model.addAttribute("basePath", BASE_PATH);
        model.addAttribute("section", "employee");
    }

    @GetMapping
    public String list(@Valid @ModelAttribute ListQuery query, Model model) {
        ListPage listPage =
                pages.create(query.getPage(), null, query.getSort(), SORT_FIELDS, "name");
        model.addAttribute(
                "accounts", employeeService.findAll(query.getSearch(), listPage.pageable()));
        model.addAttribute("search", query.getSearch());
        model.addAttribute("sort", listPage.sort());
        return "account/list";
    }

    @GetMapping("/add")
    public String createForm(Model model) {
        model.addAttribute("account", new EmployeeRequest());
        model.addAttribute("credentials", new PasswordRequest());
        return renderForm(null, model);
    }

    @PostMapping("/add")
    public String create(
            @Valid @ModelAttribute("account") EmployeeRequest request,
            BindingResult accountErrors,
            @Valid @ModelAttribute("credentials") PasswordRequest credentials,
            BindingResult passwordErrors,
            HttpServletResponse response,
            Model model) {
        if (accountErrors.hasErrors() || passwordErrors.hasErrors()) {
            response.setStatus(HttpStatus.BAD_REQUEST.value());
            return renderForm(null, model);
        }

        try {
            employeeService.create(request, credentials);
            return REDIRECT;
        } catch (AccountException exception) {
            accountFormView.rejectDuplicate(exception, accountErrors);
            response.setStatus(BusinessErrorStatus.of(exception).value());
            return renderForm(null, model);
        }
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("account", employeeMapper.toRequest(employeeService.findById(id)));
        return renderForm(id, model);
    }

    @PostMapping("/{id}/edit")
    public String update(
            @PathVariable Long id,
            @Valid @ModelAttribute("account") EmployeeRequest request,
            BindingResult errors,
            HttpServletResponse response,
            Model model) {
        if (errors.hasErrors()) {
            response.setStatus(HttpStatus.BAD_REQUEST.value());
            return renderForm(id, model);
        }

        try {
            employeeService.update(id, request);
            return REDIRECT;
        } catch (AccountException exception) {
            accountFormView.rejectDuplicate(exception, errors);
            response.setStatus(BusinessErrorStatus.of(exception).value());
            return renderForm(id, model);
        }
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        employeeService.delete(id);
        return REDIRECT;
    }

    private String renderForm(Long id, Model model) {
        return accountFormView.render(BASE_PATH, id, model);
    }
}
