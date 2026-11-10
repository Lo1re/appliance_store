package com.epam.rd.autocode.assessment.appliances.controller;

import com.epam.rd.autocode.assessment.appliances.dto.client.ClientRequest;
import com.epam.rd.autocode.assessment.appliances.dto.user.PasswordRequest;
import com.epam.rd.autocode.assessment.appliances.exception.AccountError;
import com.epam.rd.autocode.assessment.appliances.exception.AccountException;
import com.epam.rd.autocode.assessment.appliances.mapper.ClientMapper;
import com.epam.rd.autocode.assessment.appliances.service.ClientService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ClientControllerTest {

    @Mock private ClientService service;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(
                        new ClientController(service, new ClientMapper(new ModelMapper())))
                .setControllerAdvice(new AccountExceptionAdvice())
                .build();
    }

    @Test
    void validatesBothProfileAndCredentialsBeforeCreating() throws Exception {
        mvc.perform(profile("/clients/add", " ").param("password", "GoodPassword2026!"))
                .andExpect(model().attributeHasFieldErrors("account", "name"));
        mvc.perform(profile("/clients/add", "Good Name").param("password", "short"))
                .andExpect(model().attributeHasFieldErrors("credentials", "password"));
        mvc.perform(profile("/clients/7/edit", " "))
                .andExpect(model().attributeHasFieldErrors("account", "name"));
        verifyNoInteractions(service);
    }

    @Test
    void showsDuplicateEmailOnTheFormDuringCreateAndUpdate() throws Exception {
        var duplicate = new AccountException(AccountError.EMAIL_DUPLICATE);
        when(service.create(any(ClientRequest.class), any(PasswordRequest.class))).thenThrow(duplicate);
        when(service.update(eq(7L), any(ClientRequest.class))).thenThrow(duplicate);

        mvc.perform(profile("/clients/add", "New Name").param("password", "GoodPassword2026!"))
                .andExpect(view().name(org.hamcrest.Matchers.endsWith("account/form")))
                .andExpect(model().attributeHasFieldErrorCode("account", "email", "user.email.duplicate"));
        mvc.perform(profile("/clients/7/edit", "New Name"))
                .andExpect(model().attributeHasFieldErrorCode("account", "email", "user.email.duplicate"));
    }

    @Test
    void redirectsBusinessFailureAndRendersSafeDatabaseError() throws Exception {
        when(service.update(eq(7L), any(ClientRequest.class)))
                .thenThrow(new AccountException(AccountError.CLIENT_NOT_FOUND));
        doThrow(new DataIntegrityViolationException("private value")).when(service).delete(8L);

        mvc.perform(profile("/clients/7/edit", "New Name"))
                .andExpect(redirectedUrl("/clients"))
                .andExpect(flash().attribute("errorKey", "client.not-found"));
        mvc.perform(post("/clients/8/delete"))
                .andExpect(status().isInternalServerError())
                .andExpect(view().name("error/account"));
    }

    private MockHttpServletRequestBuilder profile(String path, String name) {
        return post(path).param("name", name).param("email", "account@example.com")
                .param("card", "C-42");
    }
}
