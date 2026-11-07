package com.epam.rd.autocode.assessment.appliances.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "client")
public class Client extends User {

    @NotBlank
    @Size(max = 32)
    @Column(nullable = false, length = 32)
    private String card;

    public Client(
            Long id,
            String name,
            String email,
            String password,
            String card
    ) {
        super(id, name, email, password);
        this.card = card;
    }
}