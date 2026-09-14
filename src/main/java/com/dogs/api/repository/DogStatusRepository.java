package com.dogs.api.repository;

import com.dogs.api.model.DogStatus;

import java.util.Optional;

public interface DogStatusRepository extends LookupRepository<DogStatus> {

    Optional<DogStatus> findByCodeAndDeletedAtIsNull(String code);
}
