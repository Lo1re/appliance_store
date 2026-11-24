package com.epam.rd.autocode.assessment.appliances.repository;

import com.epam.rd.autocode.assessment.appliances.model.Orders;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface OrdersRepository extends JpaRepository<Orders, Long> {

    @Override
    @EntityGraph(attributePaths = {"client", "employee"})
    Page<Orders> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"client", "employee"})
    Page<Orders> findByClient_Id(Long clientId, Pageable pageable);

    @EntityGraph(attributePaths = {"client", "employee"})
    Page<Orders> findByApproved(Boolean approved, Pageable pageable);

    @EntityGraph(
            attributePaths = {
                "client",
                "employee",
                "orderRowSet",
                "orderRowSet.appliance",
                "orderRowSet.appliance.manufacturer"
            })
    Optional<Orders> findDetailedById(Long id);

    @EntityGraph(
            attributePaths = {
                "client",
                "employee",
                "orderRowSet",
                "orderRowSet.appliance",
                "orderRowSet.appliance.manufacturer"
            })
    Optional<Orders> findDetailedByIdAndClient_Id(Long id, Long clientId);

    boolean existsByClient_Id(Long clientId);

    boolean existsByEmployee_Id(Long employeeId);

    @EntityGraph(attributePaths = {"client", "employee"})
    Page<Orders> findByClient_IdAndApproved(Long clientId, Boolean approved, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Orders o where o.id = :id")
    Optional<Orders> findForUpdate(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Orders o where o.id = :id and o.client.id = :clientId")
    Optional<Orders> findOwnedForUpdate(@Param("id") Long id, @Param("clientId") Long clientId);
}
