package com.epam.rd.autocode.assessment.appliances.integration;

import com.epam.rd.autocode.assessment.appliances.service.ApplianceService;
import com.epam.rd.autocode.assessment.appliances.service.ClientService;
import com.epam.rd.autocode.assessment.appliances.service.EmployeeService;
import com.epam.rd.autocode.assessment.appliances.service.ManufacturerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithUserDetails;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:service-authorization-it")
class ServiceAuthorizationIntegrationTest {
    @Autowired ClientService clients;
    @Autowired EmployeeService employees;
    @Autowired ApplianceService appliances;
    @Autowired ManufacturerService manufacturers;
    private static final PageRequest PAGE = PageRequest.of(0, 10);

    @Test
    @WithUserDetails("mercury@gmail.com")
    void clientCanReadCatalogButCannotManageCatalogOrAccounts() {
        assertThat(appliances.findAll("", PAGE)).isNotNull();
        assertThat(manufacturers.findAll("", PAGE)).isNotNull();
        assertThatThrownBy(() -> clients.findAll("", PAGE)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> employees.findAll("", PAGE)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> clients.delete(1L)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> employees.delete(1L)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> appliances.delete(1L)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> manufacturers.delete(1L)).isInstanceOf(AccessDeniedException.class);
        assertThat(appliances.findById(1L)).isNotNull();
        assertThat(manufacturers.findById(1L)).isNotNull();
    }

    @Test
    @WithAnonymousUser
    void anonymousCallerCannotReadProtectedServices() {
        assertThatThrownBy(() -> clients.findAll("", PAGE)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> employees.findAll("", PAGE)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> appliances.findAll("", PAGE)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> manufacturers.findAll("", PAGE)).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @WithUserDetails("phobos@gmail.com")
    void employeeCanReadAllManagementLists() {
        assertThat(clients.findAll("", PAGE)).isNotNull();
        assertThat(employees.findAll("", PAGE)).isNotNull();
        assertThat(appliances.findAll("", PAGE)).isNotNull();
        assertThat(manufacturers.findAll("", PAGE)).isNotNull();
    }
}
