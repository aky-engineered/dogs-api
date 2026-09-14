package com.dogs.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

public record LookupResponse(
        Long id,
        @JsonInclude(JsonInclude.Include.NON_NULL) String code,
        String name
) {
}
