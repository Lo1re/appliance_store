package com.epam.rd.autocode.assessment.appliances.repository;

import com.epam.rd.autocode.assessment.appliances.model.Orders;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrdersRepository extends JpaRepository<Orders, Long> {

    @Override
    @EntityGraph(attributePaths = {"client", "employee"})
    Page<Orders> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"client", "employee"})
    Page<Orders> findByClient_Id(
            Long clientId,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"client", "employee"})
    Page<Orders> findByApproved(
            Boolean approved,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {
            "client",
            "employee",
            "orderRowSet",
            "orderRowSet.appliance",
            "orderRowSet.appliance.manufacturer"
    })
    Optional<Orders> findDetailedById(Long id);

    boolean existsByClient_Id(Long clientId);

    boolean existsByEmployee_Id(Long employeeId);
}