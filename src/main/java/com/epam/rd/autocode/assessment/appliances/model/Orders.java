package com.epam.rd.autocode.assessment.appliances.model;

import com.epam.rd.autocode.assessment.appliances.exception.OrderException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "orders")
public class Orders {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id")
    private Employee employee;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @Valid
    @NotNull
    @OneToMany(
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @JoinColumn(name = "orders_id")
    private Set<OrderRow> orderRowSet = new HashSet<>();

    @NotNull
    @Column(nullable = false)
    private Boolean approved = false;

    public void assertEditable() {
        if (Boolean.TRUE.equals(approved)) {
            throw new OrderException("order.not-editable");
        }
    }

    public void replaceRows(Collection<OrderRow> rows) {
        assertEditable();
        Set<OrderRow> replacement = new HashSet<>(rows);
        orderRowSet.clear();
        orderRowSet.addAll(replacement);
    }

    public void approve(Employee employee) {
        if (Boolean.TRUE.equals(approved)) {
            throw new OrderException("order.already-approved");
        }

        this.employee = Objects.requireNonNull(employee, "Employee is required");
        approved = true;
    }
}