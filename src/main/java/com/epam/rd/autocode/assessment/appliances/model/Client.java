package com.epam.rd.autocode.assessment.appliances.model;

import com.epam.rd.autocode.assessment.appliances.validation.normalization.LoyaltyCardNumber;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "client")
@PrimaryKeyJoinColumn(name = "user_id")
public class Client extends User {

    @Size(max = 32)
    @Column(length = 32, unique = true)
    private String card;

    public void setCard(String card) {
        this.card = LoyaltyCardNumber.normalize(card);
    }

    public Client(Long id, String name, String email, String password, String card) {
        super(id, name, email, password);
        setCard(card);
    }
}
