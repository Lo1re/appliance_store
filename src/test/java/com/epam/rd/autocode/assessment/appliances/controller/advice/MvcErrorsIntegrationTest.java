package com.epam.rd.autocode.assessment.appliances.controller.advice;

import com.epam.rd.autocode.assessment.appliances.exception.AccountError;
import com.epam.rd.autocode.assessment.appliances.exception.CatalogError;
import com.epam.rd.autocode.assessment.appliances.exception.ErrorKind;
import com.epam.rd.autocode.assessment.appliances.exception.OrderError;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:mvc-errors-it;MODE=PostgreSQL;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
@Import(MvcErrorsIntegrationTest.Probes.class)
class MvcErrorsIntegrationTest {
    @Autowired MockMvc mvc;

    @Test
    @WithUserDetails("mercury@gmail.com")
    void missingResourceReturns404WithoutRedirectAndInvalidIdReturns400() throws Exception {
        mvc.perform(get("/orders/9223372036854775807"))
                .andExpect(status().isNotFound())
                .andExpect(view().name("error/404"))
                .andExpect(model().attribute("status", 404))
                .andExpect(content().string(containsString("HTTP 404")))
                .andExpect(model().attribute("errorMessageKey", "order.not-found"))
                .andExpect(header().doesNotExist("Location"));
        mvc.perform(get("/orders/abc").cookie(new Cookie("store-lang", "uk")))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("error/400"))
                .andExpect(content().string(containsString("HTTP 400")))
                .andExpect(content().string(containsString("Некоректний запит")));
    }

    @Test
    @WithUserDetails("mercury@gmail.com")
    void methodSecurityKeeps403AndUnexpectedFailureHidesItsDetails() throws Exception {
        mvc.perform(get("/orders/error-probe/employee"))
                .andExpect(status().isForbidden())
                .andExpect(view().name("error/403"));
        mvc.perform(get("/orders/error-probe/failure").cookie(new Cookie("store-lang", "uk")))
                .andExpect(status().isInternalServerError())
                .andExpect(view().name("error/5xx"))
                .andExpect(content().string(containsString("HTTP 500")))
                .andExpect(content().string(containsString("Помилка сервісу")))
                .andExpect(content().string(not(containsString("private-failure-marker"))))
                .andExpect(content().string(not(containsString("IllegalStateException"))));
    }

    @Test
    @WithUserDetails("mercury@gmail.com")
    void unsupportedMethodKeeps405AndItsAllowHeader() throws Exception {
        mvc.perform(put("/orders/1").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(view().name("error/405"))
                .andExpect(header().string("Allow", containsString("GET")));
    }

    @Test
    void containerErrorDispatchUsesLocalizedTemplatesWithoutAuthentication() throws Exception {
        mvc.perform(get("/error").accept(org.springframework.http.MediaType.TEXT_HTML).with(request -> {
                            request.setDispatcherType(DispatcherType.ERROR);
                            return request;
                        })
                        .requestAttr(RequestDispatcher.ERROR_STATUS_CODE, 403)
                        .requestAttr(RequestDispatcher.ERROR_REQUEST_URI, "/clients")
                        .cookie(new Cookie("store-lang", "uk")))
                .andExpect(status().isForbidden())
                .andExpect(view().name("error/403"))
                .andExpect(content().string(containsString("HTTP 403")))
                .andExpect(content().string(containsString("Доступ заборонено")))
                .andExpect(content().string(not(containsString("Whitelabel"))));
    }

    @Test
    void formReferenceFailuresAreValidationErrorsInsteadOfMissingRequestedResources() {
        assertThat(OrderError.NOT_FOUND.kind()).isEqualTo(ErrorKind.NOT_FOUND);
        assertThat(OrderError.CLIENT_NOT_FOUND.kind()).isEqualTo(ErrorKind.VALIDATION);
        assertThat(OrderError.APPLIANCE_NOT_FOUND.kind()).isEqualTo(ErrorKind.VALIDATION);
        assertThat(CatalogError.APPLIANCE_MANUFACTURER_NOT_FOUND.kind()).isEqualTo(ErrorKind.VALIDATION);
        assertThat(AccountError.EMAIL_DUPLICATE.kind()).isEqualTo(ErrorKind.CONFLICT);
    }

    @Test
    @WithUserDetails("mercury@gmail.com")
    void unknownAddressIs404WhileExistingForbiddenAndUnlistedRoutesStay403() throws Exception {
        mvc.perform(get("/appliances1"))
                .andExpect(status().isNotFound())
                .andExpect(header().doesNotExist("Location"));
        mvc.perform(get("/clients"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/unlisted-error-probe"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/orders/error-probe/missing"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(containsString("HTTP 404")));
    }

    @Test
    void anonymousUnknownAddressIs404ButProtectedRoutesStillRequireLogin() throws Exception {
        mvc.perform(get("/appliances1"))
                .andExpect(status().isNotFound());
        mvc.perform(get("/orders"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", endsWith("/login")));
        mvc.perform(post("/appliances1"))
                .andExpect(status().isForbidden());
    }

    @ParameterizedTest
    @ValueSource(ints = {400, 401, 403, 404, 405, 409, 429, 500, 503})
    void containerErrorsShowTheActualStatusAndHideInternalAttributes(int code) throws Exception {
        mvc.perform(get("/error").accept(org.springframework.http.MediaType.TEXT_HTML).with(request -> {
                            request.setDispatcherType(DispatcherType.ERROR);
                            return request;
                        })
                        .requestAttr(RequestDispatcher.ERROR_STATUS_CODE, code)
                        .requestAttr(RequestDispatcher.ERROR_REQUEST_URI, "/private-path")
                        .requestAttr(RequestDispatcher.ERROR_MESSAGE, "private-container-detail")
                        .requestAttr(RequestDispatcher.ERROR_EXCEPTION, new IllegalStateException("private-marker")))
                .andExpect(status().is(code))
                .andExpect(content().string(containsString("HTTP " + code)))
                .andExpect(content().string(not(containsString("private-container-detail"))))
                .andExpect(content().string(not(containsString("private-marker"))))
                .andExpect(content().string(not(containsString("IllegalStateException"))));
    }

    @Test
    @WithUserDetails("phobos@gmail.com")
    void businessConflictKeeps409AndProvidesASafeListLink() throws Exception {
        mvc.perform(get("/orders/error-probe/conflict").cookie(new Cookie("store-lang", "uk")))
                .andExpect(status().isConflict())
                .andExpect(view().name("error/409"))
                .andExpect(model().attribute("status", 409))
                .andExpect(model().attribute("returnPath", "/orders"))
                .andExpect(content().string(containsString("HTTP 409")))
                .andExpect(content().string(containsString("Конфлікт операції")));
    }

    @Test
    void jsonErrorResponseKeepsStatusAndDoesNotExposeInternalDetails() throws Exception {
        mvc.perform(get("/error").accept(org.springframework.http.MediaType.APPLICATION_JSON)
                        .with(request -> {
                            request.setDispatcherType(DispatcherType.ERROR);
                            return request;
                        })
                        .requestAttr(RequestDispatcher.ERROR_STATUS_CODE, 500)
                        .requestAttr(RequestDispatcher.ERROR_MESSAGE, "private-container-detail")
                        .requestAttr(RequestDispatcher.ERROR_EXCEPTION,
                                new IllegalStateException("private-marker")))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentTypeCompatibleWith(org.springframework.http.MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.message").doesNotExist())
                .andExpect(jsonPath("$.exception").doesNotExist())
                .andExpect(jsonPath("$.trace").doesNotExist())
                .andExpect(content().string(not(containsString("private-container-detail"))))
                .andExpect(content().string(not(containsString("private-marker"))));
    }
    @TestConfiguration
    static class Probes {
        @Bean
        FailureEndpoints failureEndpoints() {
            return new FailureEndpoints();
        }
    }

    @TestComponent
    @Controller
    @ControllerSection(path = "/orders", errorView = "error/general")
    public static class FailureEndpoints {
        @GetMapping("/orders/error-probe/conflict")
        public String conflict() {
            throw new com.epam.rd.autocode.assessment.appliances.exception.OrderException(OrderError.NOT_EDITABLE);
        }

        @GetMapping("/unlisted-error-probe")
        public String unlisted() {
            return "index";
        }

        @GetMapping("/orders/error-probe/failure")
        public String fail() {
            throw new IllegalStateException("private-failure-marker");
        }

        @GetMapping("/orders/error-probe/employee")
        @PreAuthorize("hasRole('EMPLOYEE')")
        public String employeeOnly() {
            return "index";
        }
    }
}
