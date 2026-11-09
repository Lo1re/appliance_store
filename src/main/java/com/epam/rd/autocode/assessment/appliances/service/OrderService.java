package com.epam.rd.autocode.assessment.appliances.service;

import com.epam.rd.autocode.assessment.appliances.dto.order.OrderRequest;
import com.epam.rd.autocode.assessment.appliances.dto.order.OrderResponse;
import com.epam.rd.autocode.assessment.appliances.dto.order.OrderSummary;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface OrderService {

    Page<OrderSummary> findAll(Boolean approved, Pageable pageable);

    OrderResponse findById(Long orderId);

    OrderResponse create(
            @Positive Long clientId,
            @NotNull @Valid OrderRequest request
    );

    OrderResponse update(Long orderId, @NotNull @Valid OrderRequest request);

    void delete(Long orderId);

    OrderResponse approve(Long orderId);
}