package com.dogs.api.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "dog_status")
public class DogStatus extends LookupEntity {
}
