package com.dogs.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@MappedSuperclass
public abstract class LookupEntity extends BaseEntity {

    @Column(name = "name", nullable = false, length = 100)
    private String name;
}
