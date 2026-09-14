package com.dogs.api.dto;

public record DogFilter(
        String name,
        String breed,
        String supplier
) {

    public static DogFilter empty() {
        return new DogFilter(null, null, null);
    }
}
