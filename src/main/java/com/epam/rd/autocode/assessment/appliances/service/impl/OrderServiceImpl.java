package com.epam.rd.autocode.assessment.appliances.service.impl;

import com.epam.rd.autocode.assessment.appliances.aspect.Loggable;
import com.epam.rd.autocode.assessment.appliances.dto.order.OrderRequest;
import com.epam.rd.autocode.assessment.appliances.dto.order.OrderResponse;
import com.epam.rd.autocode.assessment.appliances.dto.order.OrderSummary;
import com.epam.rd.autocode.assessment.appliances.exception.OrderError;
import com.epam.rd.autocode.assessment.appliances.exception.OrderException;
import com.epam.rd.autocode.assessment.appliances.mapper.OrderMapper;
import com.epam.rd.autocode.assessment.appliances.model.Client;
import com.epam.rd.autocode.assessment.appliances.model.Employee;
import com.epam.rd.autocode.assessment.appliances.model.OrderRow;
import com.epam.rd.autocode.assessment.appliances.model.Orders;
import com.epam.rd.autocode.assessment.appliances.model.User;
import com.epam.rd.autocode.assessment.appliances.repository.ClientRepository;
import com.epam.rd.autocode.assessment.appliances.repository.OrdersRepository;
import com.epam.rd.autocode.assessment.appliances.security.CurrentActor;
import com.epam.rd.autocode.assessment.appliances.security.OrderAccessPolicy;
import com.epam.rd.autocode.assessment.appliances.service.OrderService;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('CLIENT', 'EMPLOYEE')")
@Transactional(readOnly = true)
public class OrderServiceImpl implements OrderService {

    private final OrdersRepository ordersRepository;
    private final CurrentActor currentActor;
    private final OrderAccessPolicy orderAccessPolicy;
    private final ClientRepository clientRepository;
    private final OrderRowsFactory orderRowsFactory;
    private final OrderMapper orderMapper;

    @Override
    public Page<OrderSummary> findAll(Boolean approved, Pageable pageable) {
        User currentUser = currentActor.requireUser();
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
        Orders order = requireAccessibleOrder(orderId);
        return orderMapper.toResponse(order, calculateTotal(order));
    }

    @Override
    public OrderResponse findForEditing(Long orderId) {
        Orders order = requireAccessibleOrder(orderId);
        order.assertEditable();
        return orderMapper.toResponse(order, calculateTotal(order));
    }

    @Override
    public void requireEditable(Long orderId) {
        requireAccessibleOrder(orderId).assertEditable();
    }

    private Orders requireAccessibleOrder(Long orderId) {
        User actor = currentActor.requireUser();
        Orders order = ordersRepository.findDetailedById(orderId)
                .orElseThrow(() -> new OrderException(OrderError.NOT_FOUND));
        orderAccessPolicy.requireAccess(actor, order);
        return order;
    }

    @Override
    @Transactional
    @Loggable
    public OrderResponse create(Long clientId, OrderRequest request) {
        User currentUser = currentActor.requireUser();
        Client client = resolveClient(currentUser, clientId);
        List<OrderRow> rows = orderRowsFactory.create(request);

        Orders order = new Orders();
        order.setClient(client);
        order.replaceRows(rows);

        Orders saved = ordersRepository.saveAndFlush(order);
        return orderMapper.toResponse(saved, calculateTotal(saved));
    }

    @Override
    @Transactional
    @Loggable
    public OrderResponse update(Long orderId, OrderRequest request) {
        User currentUser = currentActor.requireUser();
        Orders order = requireOrderForUpdate(orderId);
        orderAccessPolicy.requireAccess(currentUser, order);

        order.assertEditable();
        order.replaceRows(orderRowsFactory.create(request));

        ordersRepository.flush();
        return orderMapper.toResponse(order, calculateTotal(order));
    }

    @Override
    @Transactional
    @Loggable
    public void delete(Long orderId) {
        User currentUser = currentActor.requireUser();
        Orders order = requireOrderForUpdate(orderId);
        orderAccessPolicy.requireAccess(currentUser, order);
        order.assertEditable();

        ordersRepository.delete(order);
    }

    @Override
    @Transactional
    @Loggable
    @PreAuthorize("hasRole('EMPLOYEE')")
    public OrderResponse approve(Long orderId) {
        User currentUser = currentActor.requireUser();

        Employee employee = orderAccessPolicy.requireEmployee(currentUser);

        Orders order = requireOrderForUpdate(orderId);

        order.approve(employee);

        return orderMapper.toResponse(order, calculateTotal(order));
    }

    private Client resolveClient(User currentUser, Long clientId) {
        if (currentUser instanceof Client client) {
            orderAccessPolicy.requireClientSelection(client, clientId);

            return client;
        }

        orderAccessPolicy.requireEmployee(currentUser);

        if (clientId == null) {
            throw new OrderException(OrderError.CLIENT_REQUIRED);
        }

        return clientRepository.findById(clientId)
                .orElseThrow(() -> new OrderException(OrderError.CLIENT_NOT_FOUND));
    }

    private Orders requireOrderForUpdate(Long orderId) {
        return ordersRepository.findForUpdate(orderId)
                .orElseThrow(() -> new OrderException(OrderError.NOT_FOUND));
    }

    private BigDecimal calculateTotal(Orders order) {
        return order.getOrderRowSet().stream()
                .map(OrderRow::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
