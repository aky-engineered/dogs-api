package com.dogs.api.mapper;

import com.dogs.api.dto.LookupRequest;
import com.dogs.api.dto.LookupResponse;
import com.dogs.api.model.Breed;
import com.dogs.api.model.DogStatus;
import com.dogs.api.model.LeavingReason;
import com.dogs.api.model.Supplier;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

// Request DTOs only carry editable fields, so id, timestamps and deletedAt are never overwritten
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface LookupMapper {

    LookupResponse toResponse(Breed breed);

    LookupResponse toResponse(Supplier supplier);

    LookupResponse toResponse(DogStatus dogStatus);

    LookupResponse toResponse(LeavingReason leavingReason);

    Breed toBreed(LookupRequest request);

    Supplier toSupplier(LookupRequest request);

    DogStatus toDogStatus(LookupRequest request);

    LeavingReason toLeavingReason(LookupRequest request);

    void update(LookupRequest request, @MappingTarget Breed breed);

    void update(LookupRequest request, @MappingTarget Supplier supplier);

    // A code is fixed once created, so updates only ever change the name
    @Mapping(target = "code", ignore = true)
    void update(LookupRequest request, @MappingTarget DogStatus dogStatus);

    @Mapping(target = "code", ignore = true)
    void update(LookupRequest request, @MappingTarget LeavingReason leavingReason);
}
