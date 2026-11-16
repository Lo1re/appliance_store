package com.epam.rd.autocode.assessment.appliances.dto.order;

import com.epam.rd.autocode.assessment.appliances.dto.order.OrderItemRequest;
import com.epam.rd.autocode.assessment.appliances.dto.order.OrderRequest;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OrderRequestTest {
    @Test
    void orderValidatesTheCollectionAndNestedItems() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            var request = new OrderRequest();
            request.setItems(List.of());
            assertThat(validator.validate(request)).isNotEmpty();

            var item = new OrderItemRequest();
            item.setApplianceId(0L);
            item.setQuantity(10_001L);
            request.setItems(Arrays.asList(item, null));
            assertThat(validator.validate(request))
                    .extracting(v -> v.getPropertyPath().toString())
                    .contains("items[0].applianceId", "items[0].quantity", "items[1].<list element>");
            item.setApplianceId(1L);
            item.setQuantity(2L);
            request.setItems(List.of(item));
            assertThat(validator.validate(request)).isEmpty();
        }
    }
}
