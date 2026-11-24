package com.epam.rd.autocode.assessment.appliances.api.controller;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.epam.rd.autocode.assessment.appliances.dto.appliance.ApplianceResponse;
import com.epam.rd.autocode.assessment.appliances.dto.manufacturer.ManufacturerResponse;
import com.epam.rd.autocode.assessment.appliances.dto.query.ApiListQuery;
import com.epam.rd.autocode.assessment.appliances.service.*;
import com.epam.rd.autocode.assessment.appliances.web.pagination.ListPageFactory;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;

class ApiCatalogControllerBranchTest {
    @Test
    void manufacturerListAndDetailsDelegateToService() {
        var service = mock(ManufacturerService.class);
        var controller = new ApiManufacturerController(service, new ListPageFactory(10));
        when(service.findAll(any(), any())).thenReturn(Page.empty());
        var response = new ManufacturerResponse();
        when(service.findById(7L)).thenReturn(response);
        assertThat(controller.list(new ApiListQuery()).content()).isEmpty();
        assertThat(controller.details(7L)).isSameAs(response);
    }

    @Test
    void applianceDetailsDelegateToService() {
        var service = mock(ApplianceService.class);
        var controller = new ApiApplianceController(service, new ListPageFactory(10));
        var response = new ApplianceResponse();
        when(service.findById(7L)).thenReturn(response);
        assertThat(controller.details(7L)).isSameAs(response);
    }
}
