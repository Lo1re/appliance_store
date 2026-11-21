package com.epam.rd.autocode.assessment.appliances.api.controller;
import com.epam.rd.autocode.assessment.appliances.dto.order.*;
import com.epam.rd.autocode.assessment.appliances.service.OrderService;
import com.epam.rd.autocode.assessment.appliances.web.pagination.ListPageFactory;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
class ApiOrdersControllerBranchTest {
    @Test void createsResourceLocationAndDeletesThroughSharedService() {
        var service=mock(OrderService.class);var controller=new ApiOrdersController(service,new ListPageFactory(10));var form=new OrderRequest();
        var order=new OrderResponse(new OrderSummary(42L,1L,"Client",null,null,false),List.of(),BigDecimal.ZERO);
        when(service.create(1L,form)).thenReturn(order);
        var response=controller.create(new ApiOrderCreateRequest(1L,form));assertThat(response.getStatusCode().value()).isEqualTo(201);assertThat(response.getHeaders().getLocation().toString()).isEqualTo("/api/orders/42");assertThat(response.getBody()).isSameAs(order);
        assertThat(controller.delete(42L).getStatusCode().value()).isEqualTo(204);verify(service).delete(42L);
    }
    @Test void detailsAndUpdateUseTheSharedServiceResponse() {
        var service=mock(OrderService.class);var controller=new ApiOrdersController(service,new ListPageFactory(10));var form=new OrderRequest();var order=new OrderResponse(new OrderSummary(1L,2L,"Client",null,null,false),List.of(),BigDecimal.ZERO);
        when(service.findById(1L)).thenReturn(order);when(service.update(1L,form)).thenReturn(order);assertThat(controller.details(1L)).isSameAs(order);assertThat(controller.update(1L,form)).isSameAs(order);
    }

}
