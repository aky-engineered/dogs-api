package com.dogs.api.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "breed")
public class Breed extends LookupEntity {
}
