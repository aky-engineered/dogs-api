package com.dogs.api.repository;

import com.dogs.api.model.LeavingReason;

import java.util.Optional;

public interface LeavingReasonRepository extends LookupRepository<LeavingReason> {

    Optional<LeavingReason> findByCodeAndDeletedAtIsNull(String code);
}
