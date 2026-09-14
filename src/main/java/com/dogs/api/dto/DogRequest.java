package com.dogs.api.dto;

import com.dogs.api.model.Gender;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record DogRequest(
        @NotBlank @Size(max = 100) String name,
        Long breedId,
        Long supplierId,
        @Size(max = 50) String badgeId,
        Gender gender,
        @PastOrPresent LocalDate birthDate,
        @PastOrPresent LocalDate dateAcquired,
        @NotBlank String currentStatus,
        @PastOrPresent LocalDate leavingDate,
        String leavingReason,
        String kennellingCharacteristic
) {
}
