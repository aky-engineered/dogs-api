package com.dogs.api.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "leaving_reason")
public class LeavingReason extends LookupEntity {
}
