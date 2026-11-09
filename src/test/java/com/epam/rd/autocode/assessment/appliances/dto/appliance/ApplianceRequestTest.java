package com.epam.rd.autocode.assessment.appliances.dto.appliance;

import com.epam.rd.autocode.assessment.appliances.dto.appliance.ApplianceRequest;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.assertj.core.api.Assertions.assertThat;

class ApplianceRequestTest {
    @Test
    void applianceReportsMissingAndInvalidInput() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            var request = new ApplianceRequest();
            request.setName(null);
            request.setModel(null);
            request.setCharacteristic(null);
            request.setDescription(null);
            request.setManufacturerId(0L);
            request.setPower(0);
            request.setPrice(new BigDecimal("1.234"));
            assertThat(factory.getValidator().validate(request))
                    .extracting(v -> v.getPropertyPath().toString())
                    .contains("name", "model", "characteristic", "description",
                            "manufacturerId", "power", "price", "category", "powerType");
        }
    }
}
