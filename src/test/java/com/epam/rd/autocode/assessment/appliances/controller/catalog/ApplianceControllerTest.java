package com.epam.rd.autocode.assessment.appliances.controller.catalog;

import com.epam.rd.autocode.assessment.appliances.controller.advice.BusinessExceptionAdvice;
import com.epam.rd.autocode.assessment.appliances.dto.appliance.ApplianceRequest;
import com.epam.rd.autocode.assessment.appliances.dto.appliance.ApplianceResponse;
import com.epam.rd.autocode.assessment.appliances.exception.CatalogError;
import com.epam.rd.autocode.assessment.appliances.exception.CatalogException;
import com.epam.rd.autocode.assessment.appliances.mapper.ApplianceMapper;
import com.epam.rd.autocode.assessment.appliances.model.Category;
import com.epam.rd.autocode.assessment.appliances.model.PowerType;
import com.epam.rd.autocode.assessment.appliances.service.ApplianceService;
import com.epam.rd.autocode.assessment.appliances.service.lookup.LookupService;
import com.epam.rd.autocode.assessment.appliances.web.form.ApplianceFormAssembler;
import com.epam.rd.autocode.assessment.appliances.web.pagination.ListPageFactory;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ApplianceControllerTest {

    @Mock private ApplianceService service;
    @Mock private LookupService lookups;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(
                        new ApplianceController(service, new ApplianceFormAssembler(service, Mappers.getMapper(ApplianceMapper.class), lookups), new ListPageFactory(10)))
                .setControllerAdvice(new BusinessExceptionAdvice())
                .build();
    }

    @Test
    void paginatesSearchWithStableOrdering() throws Exception {
        var page = PageRequest.of(2, 10, Sort.by("price", "id"));
        when(service.findAll("claw", page)).thenReturn(Page.empty(page));

        mvc.perform(get("/appliances").param("search", "claw").param("page", "2").param("sort", "price"))
                .andExpect(status().isOk())
                .andExpect(view().name("appliance/appliances"))
                .andExpect(model().attribute("sort", "price"));
        verify(service).findAll("claw", page);
    }

    @Test
    void opensCreateAndEditWithManufacturerOptions() throws Exception {
        when(lookups.manufacturers(eq(""), any())).thenReturn(java.util.List.of());
        var product = new ApplianceResponse();
        product.setName("Samsung Galaxy S10");
        product.setManufacturerId(1L);
        product.setCategory(Category.BIG);
        product.setPowerType(PowerType.AC220);
        product.setPrice(new BigDecimal("1.01"));
        when(service.findById(7L)).thenReturn(product);

        mvc.perform(get("/appliances/add"))
                .andExpect(view().name("appliance/newAppliance"))
                .andExpect(model().attribute("editing", false));
        mvc.perform(get("/appliances/7/edit"))
                .andExpect(model().attribute("editing", true))
                .andExpect(model().attribute("formAction", "/appliances/7/edit"))
                .andExpect(model().attributeExists("manufacturers", "appliance"));
    }

    @Test
    void validatesAndKeepsFormWhenSelectedManufacturerDisappears() throws Exception {
        when(lookups.manufacturers(eq(""), any())).thenReturn(java.util.List.of());
        when(service.create(any(ApplianceRequest.class)))
                .thenThrow(new CatalogException(CatalogError.APPLIANCE_MANUFACTURER_NOT_FOUND));

        mvc.perform(post("/appliances/add"))
                .andExpect(model().attributeHasFieldErrors("appliance", "name"));
        mvc.perform(product("/appliances/add"))
                .andExpect(model().attributeHasFieldErrorCode(
                        "appliance", "manufacturerId", "appliance.manufacturer.not-found"));
        verify(service, times(1)).create(any(ApplianceRequest.class));
    }

    @Test
    void savesAndDeletesValidRequests() throws Exception {
        mvc.perform(product("/appliances/add")).andExpect(redirectedUrl("/appliances"));
        mvc.perform(product("/appliances/7/edit")).andExpect(redirectedUrl("/appliances"));
        mvc.perform(post("/appliances/7/delete")).andExpect(redirectedUrl("/appliances"));
        verify(service).create(argThat(r -> r.getManufacturerId() == 1L));
        verify(service).update(eq(7L), any(ApplianceRequest.class));
        verify(service).delete(7L);
    }

    @Test
    void rendersMissingResourceAndSafeDatabaseErrors() throws Exception {
        when(service.update(eq(99L), any(ApplianceRequest.class)))
                .thenThrow(new CatalogException(CatalogError.APPLIANCE_NOT_FOUND));
        doThrow(new DataIntegrityViolationException("private value")).when(service).delete(8L);

        mvc.perform(product("/appliances/99/edit"))
                .andExpect(status().isNotFound())
                .andExpect(view().name("error/404"))
                .andExpect(model().attribute("errorMessageKey", "appliance.not-found"));
        mvc.perform(post("/appliances/8/delete"))
                .andExpect(status().isInternalServerError())
                .andExpect(view().name("error/catalog"));
    }

    @Test
    void productDetailsExposeTheDtoAndRoleBasedManagementActions() throws Exception {
        var product = new ApplianceResponse();
        product.setId(7L);
        product.setName("Laptop");
        when(service.findById(7L)).thenReturn(product);
        mvc.perform(get("/appliances/7").param("lang", "uk"))
                .andExpect(view().name("appliance/details"))
                .andExpect(model().attribute("appliance", product))
                .andExpect(model().attribute("canManage", false));
        mvc.perform(get("/appliances/7").with(request -> {
            request.addUserRole("EMPLOYEE");
            return request;
        })).andExpect(model().attribute("canManage", true));
    }

    private MockHttpServletRequestBuilder product(String path) {
        return post(path).param("name", "Product").param("model", "M1")
                .param("manufacturerId", "1").param("category", "SMALL")
                .param("powerType", "AC220").param("characteristic", "Compact")
                .param("description", "Product description").param("power", "100")
                .param("price", "1.01");
    }
}
