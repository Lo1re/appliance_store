package com.epam.rd.autocode.assessment.appliances.service.impl.order;

import com.epam.rd.autocode.assessment.appliances.dto.order.OrderItemRequest;
import com.epam.rd.autocode.assessment.appliances.dto.order.OrderRequest;
import com.epam.rd.autocode.assessment.appliances.exception.OrderError;
import com.epam.rd.autocode.assessment.appliances.exception.OrderException;
import com.epam.rd.autocode.assessment.appliances.model.Appliance;
import com.epam.rd.autocode.assessment.appliances.model.OrderRow;
import com.epam.rd.autocode.assessment.appliances.repository.ApplianceRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderRowsFactory {
    private final ApplianceRepository applianceRepository;

    public List<OrderRow> create(OrderRequest request) {
        Map<Long, Long> quantities = new LinkedHashMap<>();

        for (OrderItemRequest item : request.getItems()) {
            Long previous = quantities.putIfAbsent(
                    item.getApplianceId(), item.getQuantity()
            );

            if (previous != null) {
                throw new OrderException(OrderError.DUPLICATE_APPLIANCE);
            }
        }

        List<Appliance> appliances = applianceRepository.findAllById(quantities.keySet());

        if (appliances.size() != quantities.size()) {
            throw new OrderException(OrderError.APPLIANCE_NOT_FOUND);
        }

        List<OrderRow> rows = new ArrayList<>(appliances.size());

        for (Appliance appliance : appliances) {
            Long quantity = quantities.get(appliance.getId());
            BigDecimal amount = appliance.getPrice().multiply(BigDecimal.valueOf(quantity));

            rows.add(new OrderRow(null, appliance, quantity, amount));
        }

        return rows;
    }

}
