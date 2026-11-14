package com.epam.rd.autocode.assessment.appliances.service.impl;

import com.epam.rd.autocode.assessment.appliances.security.StorePrincipal;
import com.epam.rd.autocode.assessment.appliances.dto.order.OrderItemRequest;
import com.epam.rd.autocode.assessment.appliances.dto.order.OrderRequest;
import com.epam.rd.autocode.assessment.appliances.dto.order.OrderResponse;
import com.epam.rd.autocode.assessment.appliances.dto.order.OrderSummary;
import com.epam.rd.autocode.assessment.appliances.exception.OrderException;
import com.epam.rd.autocode.assessment.appliances.mapper.OrderMapper;
import com.epam.rd.autocode.assessment.appliances.model.Appliance;
import com.epam.rd.autocode.assessment.appliances.model.Client;
import com.epam.rd.autocode.assessment.appliances.model.Employee;
import com.epam.rd.autocode.assessment.appliances.model.OrderRow;
import com.epam.rd.autocode.assessment.appliances.model.Orders;
import com.epam.rd.autocode.assessment.appliances.model.User;
import com.epam.rd.autocode.assessment.appliances.repository.ApplianceRepository;
import com.epam.rd.autocode.assessment.appliances.repository.ClientRepository;
import com.epam.rd.autocode.assessment.appliances.repository.OrdersRepository;
import com.epam.rd.autocode.assessment.appliances.repository.UserRepository;
import com.epam.rd.autocode.assessment.appliances.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Validated
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('CLIENT', 'EMPLOYEE')")
@Transactional(readOnly = true)
public class OrderServiceImpl implements OrderService {

    private final OrdersRepository ordersRepository;
    private final UserRepository userRepository;
    private final ClientRepository clientRepository;
    private final ApplianceRepository applianceRepository;
    private final OrderMapper orderMapper;

    @Override
    public Page<OrderSummary> findAll(Boolean approved, Pageable pageable) {
        User currentUser = requireCurrentUser();
        Page<Orders> orders;

        if (currentUser instanceof Client client) {
            orders = approved == null
                    ? ordersRepository.findByClient_Id(client.getId(), pageable)
                    : ordersRepository.findByClient_IdAndApproved(
                    client.getId(), approved, pageable
            );
        } else {
            orders = approved == null
                    ? ordersRepository.findAll(pageable)
                    : ordersRepository.findByApproved(approved, pageable);
        }

        return orders.map(orderMapper::toSummary);
    }

    @Override
    public OrderResponse findById(Long orderId) {
        User currentUser = requireCurrentUser();
        Orders order = ordersRepository.findDetailedById(orderId)
                .orElseThrow(() -> new OrderException("order.not-found"));

        requireOrderAccess(currentUser, order);
        return orderMapper.toResponse(order, calculateTotal(order));
    }

    @Override
    @Transactional
    public OrderResponse create(Long clientId, OrderRequest request) {
        User currentUser = requireCurrentUser();
        Client client = resolveClient(currentUser, clientId);
        List<OrderRow> rows = buildRows(request);

        Orders order = new Orders();
        order.setClient(client);
        order.getOrderRowSet().addAll(rows);

        Orders saved = ordersRepository.saveAndFlush(order);
        return orderMapper.toResponse(saved, calculateTotal(saved));
    }

    @Override
    @Transactional
    public OrderResponse update(Long orderId, OrderRequest request) {
        User currentUser = requireCurrentUser();
        Orders order = requireOrderForUpdate(orderId);
        requireOrderAccess(currentUser, order);

        List<OrderRow> replacementRows = buildRows(request);
        order.getOrderRowSet().clear();
        order.getOrderRowSet().addAll(replacementRows);
        order.setApproved(false);
        order.setEmployee(null);

        ordersRepository.flush();
        return orderMapper.toResponse(order, calculateTotal(order));
    }

    @Override
    @Transactional
    public void delete(Long orderId) {
        User currentUser = requireCurrentUser();
        Orders order = requireOrderForUpdate(orderId);
        requireOrderAccess(currentUser, order);

        ordersRepository.delete(order);
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('EMPLOYEE')")
    public OrderResponse approve(Long orderId) {
        User currentUser = requireCurrentUser();

        if (!(currentUser instanceof Employee employee)) {
            throw new OrderException("order.access-denied");
        }

        Orders order = requireOrderForUpdate(orderId);

        if (Boolean.TRUE.equals(order.getApproved())) {
            throw new OrderException("order.already-approved");
        }

        order.setEmployee(employee);
        order.setApproved(true);

        return orderMapper.toResponse(order, calculateTotal(order));
    }

    private User requireCurrentUser() {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof StorePrincipal principal)) {
            throw new OrderException("order.access-denied");
        }

        User user = userRepository.findById(principal.getAccountId())
                .orElseThrow(() -> new OrderException("order.access-denied"));

        String expectedRole;

        if (user instanceof Client) {
            expectedRole = "ROLE_CLIENT";
        } else if (user instanceof Employee) {
            expectedRole = "ROLE_EMPLOYEE";
        } else {
            throw new OrderException("order.access-denied");
        }

        boolean roleMatches = authentication.getAuthorities().stream()
                .anyMatch(authority -> expectedRole.equals(authority.getAuthority()));

        if (!roleMatches) {
            throw new OrderException("order.access-denied");
        }

        return user;
    }

    private Client resolveClient(User currentUser, Long clientId) {
        if (currentUser instanceof Client client) {
            if (clientId != null && !client.getId().equals(clientId)) {
                throw new OrderException("order.access-denied");
            }

            return client;
        }

        if (clientId == null) {
            throw new OrderException("order.client.required");
        }

        return clientRepository.findById(clientId)
                .orElseThrow(() -> new OrderException("order.client.not-found"));
    }

    private Orders requireOrderForUpdate(Long orderId) {
        return ordersRepository.findForUpdate(orderId)
                .orElseThrow(() -> new OrderException("order.not-found"));
    }

    private void requireOrderAccess(User currentUser, Orders order) {
        if (currentUser instanceof Client client
                && !client.getId().equals(order.getClient().getId())) {
            throw new OrderException("order.not-found");
        }
    }

    private List<OrderRow> buildRows(OrderRequest request) {
        Map<Long, Long> quantities = new LinkedHashMap<>();

        for (OrderItemRequest item : request.getItems()) {
            Long previous = quantities.putIfAbsent(
                    item.getApplianceId(), item.getQuantity()
            );

            if (previous != null) {
                throw new OrderException("order.appliance.duplicate");
            }
        }

        List<Appliance> appliances = applianceRepository.findAllById(quantities.keySet());

        if (appliances.size() != quantities.size()) {
            throw new OrderException("order.appliance.not-found");
        }

        List<OrderRow> rows = new ArrayList<>(appliances.size());

        for (Appliance appliance : appliances) {
            Long quantity = quantities.get(appliance.getId());
            BigDecimal amount = appliance.getPrice().multiply(BigDecimal.valueOf(quantity));

            rows.add(new OrderRow(null, appliance, quantity, amount));
        }

        return rows;
    }

    private BigDecimal calculateTotal(Orders order) {
        return order.getOrderRowSet().stream()
                .map(OrderRow::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}