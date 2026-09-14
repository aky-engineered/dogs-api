package com.dogs.api.mapper;

import com.dogs.api.dto.DogRequest;
import com.dogs.api.dto.DogResponse;
import com.dogs.api.model.Dog;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", uses = LookupMapper.class, injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public interface DogMapper {

    @Mapping(target = "currentStatus", source = "status")
    DogResponse toResponse(Dog dog);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "breed", ignore = true)
    @Mapping(target = "supplier", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "leavingReason", ignore = true)
    @Mapping(target = "gender", defaultValue = "UNKNOWN")
    void update(DogRequest request, @MappingTarget Dog dog);
}
