package com.dogs.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "dog")
public class Dog extends BaseEntity {

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "breed_id")
    private Breed breed;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id")
    private Supplier supplier;

    @Column(name = "badge_id", length = 50)
    private String badgeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", nullable = false, length = 10)
    private Gender gender = Gender.UNKNOWN;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(name = "date_acquired")
    private LocalDate dateAcquired;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "status_id", nullable = false)
    private DogStatus status;

    @Column(name = "leaving_date")
    private LocalDate leavingDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "leaving_reason_id")
    private LeavingReason leavingReason;

    @Column(name = "kennelling_characteristic", columnDefinition = "TEXT")
    private String kennellingCharacteristic;
}
