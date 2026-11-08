package com.epam.rd.autocode.assessment.appliances.repository;

import com.epam.rd.autocode.assessment.appliances.model.Appliance;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ApplianceRepository
        extends JpaRepository<Appliance, Long> {

    @EntityGraph(attributePaths = "manufacturer")
    @Query("""
            select a
            from Appliance a
            where lower(a.name) like lower(
                concat('%', concat(:search, '%'))
            )
            or lower(a.model) like lower(
                concat('%', concat(:search, '%'))
            )
            or lower(a.manufacturer.name) like lower(
                concat('%', concat(:search, '%'))
            )
            """)
    Page<Appliance> search(
            @Param("search") String search,
            Pageable pageable
    );

    boolean existsByManufacturer_Id(Long manufacturerId);
}