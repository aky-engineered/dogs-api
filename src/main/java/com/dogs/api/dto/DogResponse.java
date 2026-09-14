package com.dogs.api.dto;

import com.dogs.api.model.Gender;

import java.time.Instant;
import java.time.LocalDate;

public record DogResponse(
        Long id,
        String name,
        LookupResponse breed,
        LookupResponse supplier,
        String badgeId,
        Gender gender,
        LocalDate birthDate,
        LocalDate dateAcquired,
        LookupResponse currentStatus,
        LocalDate leavingDate,
        LookupResponse leavingReason,
        String kennellingCharacteristic,
        Instant createdAt,
        Instant updatedAt,
        Instant deletedAt
) {
}
