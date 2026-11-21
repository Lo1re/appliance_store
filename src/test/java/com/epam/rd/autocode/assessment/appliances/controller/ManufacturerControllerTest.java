package com.epam.rd.autocode.assessment.appliances.controller;

import com.epam.rd.autocode.assessment.appliances.dto.manufacturer.ManufacturerRequest;
import com.epam.rd.autocode.assessment.appliances.dto.manufacturer.ManufacturerResponse;
import com.epam.rd.autocode.assessment.appliances.exception.CatalogError;
import com.epam.rd.autocode.assessment.appliances.exception.CatalogException;
import com.epam.rd.autocode.assessment.appliances.mapper.ManufacturerMapper;
import com.epam.rd.autocode.assessment.appliances.service.ManufacturerService;
import com.epam.rd.autocode.assessment.appliances.web.pagination.ListPageFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ManufacturerControllerTest {

    @Mock
    private ManufacturerService service;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        var mapper = Mappers.getMapper(ManufacturerMapper.class);
        mvc = MockMvcBuilders.standaloneSetup(new ManufacturerController(service, mapper, new ListPageFactory(10)))
                .setControllerAdvice(new BusinessExceptionAdvice())
                .build();
    }

    @Test
    void listsWithAllowedSortingAndNormalizesInvalidOptions() throws Exception {
        var byId = PageRequest.of(2, 10, Sort.by("id"));
        var byName = PageRequest.of(0, 10, Sort.by("name", "id"));
        when(service.findAll("sam", byId)).thenReturn(Page.empty(byId));
        when(service.findAll("", byName)).thenReturn(Page.empty(byName));

        mvc.perform(get("/manufacturers")
                        .param("search", "sam").param("page", "2").param("sort", "id"))
                .andExpect(status().isOk())
                .andExpect(view().name("manufacture/manufacturers"))
                .andExpect(model().attribute("search", "sam"))
                .andExpect(model().attribute("sort", "id"));

        mvc.perform(get("/manufacturers").param("page", "-1").param("sort", "unknown"))
                .andExpect(model().attribute("sort", "name"));

        verify(service).findAll("sam", byId);
        verify(service).findAll("", byName);
    }

    @Test
    void opensCreateAndEditForms() throws Exception {
        var existing = new ManufacturerResponse();
        existing.setName("Samsung");
        when(service.findById(7L)).thenReturn(existing);

        mvc.perform(get("/manufacturers/add"))
                .andExpect(status().isOk())
                .andExpect(view().name("manufacture/newManufacturer"))
                .andExpect(model().attribute("editing", false));

        mvc.perform(get("/manufacturers/7/edit"))
                .andExpect(status().isOk())
                .andExpect(view().name("manufacture/newManufacturer"))
                .andExpect(model().attribute("editing", true))
                .andExpect(model().attribute("formAction", "/manufacturers/7/edit"))
                .andExpect(model().attributeExists("manufacturer"));
    }

    @Test
    void rejectsBlankNameBeforeCallingService() throws Exception {
        mvc.perform(post("/manufacturers/add").param("name", "   "))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("manufacturer", "name"));

        verifyNoInteractions(service);
    }

    @Test
    void createsAndUpdatesUsingIdFromUrl() throws Exception {
        mvc.perform(post("/manufacturers/add").param("name", "  New Brand  "))
                .andExpect(redirectedUrl("/manufacturers"));

        mvc.perform(post("/manufacturers/7/edit").param("name", "Renamed"))
                .andExpect(redirectedUrl("/manufacturers"));

        verify(service).create(argThat(request -> "New Brand".equals(request.getName())));
        verify(service).update(eq(7L), argThat(request -> "Renamed".equals(request.getName())));
    }

    @Test
    void showsDuplicateAsFieldErrorAndMissingRecordAsNotFound() throws Exception {
        when(service.create(any(ManufacturerRequest.class)))
                .thenThrow(new CatalogException(CatalogError.MANUFACTURER_DUPLICATE_NAME));
        when(service.update(eq(99L), any(ManufacturerRequest.class)))
                .thenThrow(new CatalogException(CatalogError.MANUFACTURER_NOT_FOUND));

        mvc.perform(post("/manufacturers/add").param("name", "Samsung"))
                .andExpect(view().name("manufacture/newManufacturer"))
                .andExpect(model().attributeHasFieldErrorCode(
                        "manufacturer", "name", "manufacturer.name.duplicate"));

        mvc.perform(post("/manufacturers/99/edit").param("name", "Renamed"))
                .andExpect(status().isNotFound())
                .andExpect(view().name("error/404"))
                .andExpect(model().attribute("errorMessageKey", "manufacturer.not-found"));
    }

    @Test
    void deletesManufacturerAndRedirectsToList() throws Exception {
        mvc.perform(post("/manufacturers/7/delete"))
                .andExpect(redirectedUrl("/manufacturers"));

        verify(service).delete(7L);
    }

    @Test
    void showsErrorPageForDatabaseFailureOnDelete() throws Exception {
        doThrow(new DataIntegrityViolationException("constraint"))
                .when(service).delete(8L);

        mvc.perform(post("/manufacturers/8/delete"))
                .andExpect(status().isInternalServerError())
                .andExpect(view().name("error/catalog"));

        verify(service).delete(8L);
    }
}
