package com.epam.rd.autocode.assessment.appliances.repository;

import com.epam.rd.autocode.assessment.appliances.model.Employee;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import jakarta.persistence.LockModeType;
import java.util.List;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select employee from Employee employee order by employee.id")
    List<Employee> findAllForDeletionLock();

    Page<Employee> findByNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
            String name,
            String email,
            Pageable pageable
    );
}
