package com.dogs.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// code only applies to statuses and leaving reasons - the services enforce when it is required or forbidden
public record LookupRequest(
        @Size(max = 50)
        @Pattern(regexp = "^[A-Z][A-Z0-9_]*$", message = "must be upper case letters, digits and underscores, e.g. IN_TRAINING")
        String code,
        @NotBlank @Size(max = 100) String name
) {
}
