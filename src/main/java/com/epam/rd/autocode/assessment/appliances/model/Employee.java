package com.epam.rd.autocode.assessment.appliances.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "employee")
@PrimaryKeyJoinColumn(name = "user_id")
public class Employee extends User {

    @NotBlank
    @Size(max = 100)
    @Column(nullable = false, length = 100)
    private String department;

    public Employee(
            Long id,
            String name,
            String email,
            String password,
            String department
    ) {
        super(id, name, email, password);
        this.department = department;
    }
}
