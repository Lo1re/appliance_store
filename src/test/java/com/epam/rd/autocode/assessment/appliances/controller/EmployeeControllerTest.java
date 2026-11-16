package com.epam.rd.autocode.assessment.appliances.controller;

import com.epam.rd.autocode.assessment.appliances.dto.employee.EmployeeRequest;
import com.epam.rd.autocode.assessment.appliances.dto.user.PasswordRequest;
import com.epam.rd.autocode.assessment.appliances.exception.AccountError;
import com.epam.rd.autocode.assessment.appliances.exception.AccountException;
import com.epam.rd.autocode.assessment.appliances.mapper.EmployeeMapper;
import com.epam.rd.autocode.assessment.appliances.service.EmployeeService;
import com.epam.rd.autocode.assessment.appliances.web.form.AccountFormView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class EmployeeControllerTest {

    @Mock private EmployeeService service;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(
                        new EmployeeController(service, Mappers.getMapper(EmployeeMapper.class), new AccountFormView()))
                .setControllerAdvice(new BusinessExceptionAdvice())
                .build();
    }

    @Test
    void validatesBothProfileAndCredentialsBeforeCreating() throws Exception {
        mvc.perform(profile("/employees/add", " ").param("password", "GoodPassword2026!"))
                .andExpect(model().attributeHasFieldErrors("account", "name"));
        mvc.perform(profile("/employees/add", "Good Name").param("password", "short"))
                .andExpect(model().attributeHasFieldErrors("credentials", "password"));
        mvc.perform(profile("/employees/7/edit", " "))
                .andExpect(model().attributeHasFieldErrors("account", "name"));
        verifyNoInteractions(service);
    }

    @Test
    void showsDuplicateEmailOnTheFormDuringCreateAndUpdate() throws Exception {
        var duplicate = new AccountException(AccountError.EMAIL_DUPLICATE);
        when(service.create(any(EmployeeRequest.class), any(PasswordRequest.class))).thenThrow(duplicate);
        when(service.update(eq(7L), any(EmployeeRequest.class))).thenThrow(duplicate);

        mvc.perform(profile("/employees/add", "New Name").param("password", "GoodPassword2026!"))
                .andExpect(view().name(org.hamcrest.Matchers.endsWith("account/form")))
                .andExpect(model().attributeHasFieldErrorCode("account", "email", "user.email.duplicate"));
        mvc.perform(profile("/employees/7/edit", "New Name"))
                .andExpect(model().attributeHasFieldErrorCode("account", "email", "user.email.duplicate"));
    }

    @Test
    void rendersNotFoundAndSafeDatabaseErrors() throws Exception {
        when(service.update(eq(7L), any(EmployeeRequest.class)))
                .thenThrow(new AccountException(AccountError.EMPLOYEE_NOT_FOUND));
        doThrow(new DataIntegrityViolationException("private value")).when(service).delete(8L);

        mvc.perform(profile("/employees/7/edit", "New Name"))
                .andExpect(status().isNotFound())
                .andExpect(view().name("error/404"))
                .andExpect(model().attribute("errorMessageKey", "employee.not-found"));
        mvc.perform(post("/employees/8/delete"))
                .andExpect(status().isInternalServerError())
                .andExpect(view().name("error/account"));
    }

    private MockHttpServletRequestBuilder profile(String path, String name) {
        return post(path).param("name", name).param("email", "account@example.com")
                .param("department", "Stock");
    }
}
