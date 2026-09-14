package com.dogs.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "breed")
public class Breed extends BaseEntity {

    @Column(name = "name", nullable = false, length = 100)
    private String name;
}
