package com.epam.rd.autocode.assessment.appliances.service.impl.order;

import com.epam.rd.autocode.assessment.appliances.dto.order.OrderItemRequest;
import com.epam.rd.autocode.assessment.appliances.dto.order.OrderRequest;
import com.epam.rd.autocode.assessment.appliances.exception.OrderError;
import com.epam.rd.autocode.assessment.appliances.exception.OrderException;
import com.epam.rd.autocode.assessment.appliances.model.Appliance;
import com.epam.rd.autocode.assessment.appliances.repository.ApplianceRepository;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class OrderRowsFactoryTest {
    private final ApplianceRepository appliances = mock(ApplianceRepository.class);
    private final OrderRowsFactory factory = new OrderRowsFactory(appliances);

    @Test
    void snapshotsLineAmountsUsingPersistedPricesAndRequestedQuantities() {
        var first = appliance(1L, "12.35");
        var second = appliance(2L, "8.00");
        when(appliances.findAllById(any())).thenReturn(List.of(second, first));
        var rows = factory.create(request(item(1L, 3L), item(2L, 2L)));
        assertThat(rows).hasSize(2);
        assertThat(rows).filteredOn(row -> row.getAppliance().getId().equals(1L))
                .singleElement().satisfies(row -> {
                    assertThat(row.getId()).isNull();
                    assertThat(row.getNumber()).isEqualTo(3L);
                    assertThat(row.getAmount()).isEqualByComparingTo("37.05");
                });
        first.setPrice(new BigDecimal("99.00"));
        assertThat(rows).filteredOn(row -> row.getAppliance().getId().equals(1L))
                .singleElement().satisfies(row ->
                        assertThat(row.getAmount()).isEqualByComparingTo("37.05"));
    }

    @Test
    void rejectsDuplicateProductsBeforeLookingThemUp() {
        assertThatThrownBy(() -> factory.create(request(item(1L, 2L), item(1L, 3L))))
                .isInstanceOfSatisfying(OrderException.class,
                        error -> assertThat(error.error()).isEqualTo(OrderError.DUPLICATE_APPLIANCE));
        verifyNoInteractions(appliances);
    }

    @Test
    void rejectsARequestWhenAnyProductIsMissing() {
        when(appliances.findAllById(any())).thenReturn(List.of(appliance(1L, "10.00")));
        assertThatThrownBy(() -> factory.create(request(item(1L, 1L), item(2L, 1L))))
                .isInstanceOfSatisfying(OrderException.class,
                        error -> assertThat(error.error()).isEqualTo(OrderError.APPLIANCE_NOT_FOUND));
    }

    private Appliance appliance(Long id, String price) {
        var appliance = new Appliance();
        appliance.setId(id);
        appliance.setPrice(new BigDecimal(price));
        return appliance;
    }

    private OrderItemRequest item(Long id, Long quantity) {
        var item = new OrderItemRequest();
        item.setApplianceId(id);
        item.setQuantity(quantity);
        return item;
    }

    private OrderRequest request(OrderItemRequest... items) {
        var request = new OrderRequest();
        request.setItems(List.of(items));
        return request;
    }
}
