package com.epam.rd.autocode.assessment.appliances.service.impl.order;

import com.epam.rd.autocode.assessment.appliances.aspect.Loggable;
import com.epam.rd.autocode.assessment.appliances.dto.order.OrderRequest;
import com.epam.rd.autocode.assessment.appliances.dto.order.OrderResponse;
import com.epam.rd.autocode.assessment.appliances.dto.order.OrderSummary;
import com.epam.rd.autocode.assessment.appliances.exception.OrderError;
import com.epam.rd.autocode.assessment.appliances.exception.OrderException;
import com.epam.rd.autocode.assessment.appliances.logging.BusinessAction;
import com.epam.rd.autocode.assessment.appliances.logging.BusinessAudit;
import com.epam.rd.autocode.assessment.appliances.mapper.OrderMapper;
import com.epam.rd.autocode.assessment.appliances.model.Client;
import com.epam.rd.autocode.assessment.appliances.model.Employee;
import com.epam.rd.autocode.assessment.appliances.model.OrderRow;
import com.epam.rd.autocode.assessment.appliances.model.Orders;
import com.epam.rd.autocode.assessment.appliances.model.User;
import com.epam.rd.autocode.assessment.appliances.persistence.PersistenceBoundary;
import com.epam.rd.autocode.assessment.appliances.persistence.PersistenceOperation;
import com.epam.rd.autocode.assessment.appliances.repository.ClientRepository;
import com.epam.rd.autocode.assessment.appliances.repository.OrdersRepository;
import com.epam.rd.autocode.assessment.appliances.security.authorization.OrderAccessPolicy;
import com.epam.rd.autocode.assessment.appliances.security.identity.CurrentActor;
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

    private final BusinessAudit audit;

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
        var accessible = actor instanceof Client client
                ? ordersRepository.findDetailedByIdAndClient_Id(orderId, client.getId())
                : ordersRepository.findDetailedById(orderId);
        Orders order = accessible.orElseThrow(() -> new OrderException(OrderError.NOT_FOUND));
        orderAccessPolicy.requireAccess(actor, order);
        return order;
    }

    @Override
    @Transactional
    @Loggable
    @PersistenceBoundary(PersistenceOperation.ORDER_WRITE)
    public OrderResponse create(Long clientId, OrderRequest request) {
        User currentUser = currentActor.requireUser();
        Client client = resolveClient(currentUser, clientId);
        List<OrderRow> rows = orderRowsFactory.create(request);

        Orders order = new Orders();
        order.setClient(client);
        order.replaceRows(rows);

        Orders saved = ordersRepository.saveAndFlush(order);
        audit.record(BusinessAction.ORDER_CREATED, saved.getId());
        return orderMapper.toResponse(saved, calculateTotal(saved));
    }

    @Override
    @Transactional
    @Loggable
    @PersistenceBoundary(PersistenceOperation.ORDER_WRITE)
    public OrderResponse update(Long orderId, OrderRequest request) {
        User currentUser = currentActor.requireUser();
        Orders order = requireOrderForUpdate(orderId, currentUser);
        orderAccessPolicy.requireAccess(currentUser, order);

        order.assertEditable();
        order.replaceRows(orderRowsFactory.create(request));

        ordersRepository.flush();
        audit.record(BusinessAction.ORDER_UPDATED, order.getId());
        return orderMapper.toResponse(order, calculateTotal(order));
    }

    @Override
    @Transactional
    @Loggable
    @PersistenceBoundary(PersistenceOperation.ORDER_DELETE)
    public void delete(Long orderId) {
        User currentUser = currentActor.requireUser();
        Orders order = requireOrderForUpdate(orderId, currentUser);
        orderAccessPolicy.requireAccess(currentUser, order);
        order.assertEditable();

        ordersRepository.delete(order);
        audit.record(BusinessAction.ORDER_DELETED, order.getId());
    }

    @Override
    @Transactional
    @Loggable
    @PreAuthorize("hasRole('EMPLOYEE')")
    @PersistenceBoundary(PersistenceOperation.ORDER_WRITE)
    public OrderResponse approve(Long orderId) {
        User currentUser = currentActor.requireUser();

        Employee employee = orderAccessPolicy.requireEmployee(currentUser);

        Orders order = requireOrderForUpdate(orderId, currentUser);

        order.approve(employee);
        audit.record(BusinessAction.ORDER_APPROVED, order.getId());

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

    private Orders requireOrderForUpdate(Long orderId, User actor) {
        var accessible = actor instanceof Client client
                ? ordersRepository.findOwnedForUpdate(orderId, client.getId())
                : ordersRepository.findForUpdate(orderId);
        return accessible.orElseThrow(() -> new OrderException(OrderError.NOT_FOUND));
    }

    private BigDecimal calculateTotal(Orders order) {
        return order.getOrderRowSet().stream()
                .map(OrderRow::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
