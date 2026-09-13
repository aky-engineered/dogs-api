package com.dogs.api.repository;

import com.dogs.api.model.Dog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DogRepository extends JpaRepository<Dog, Long> {

    Page<Dog> findAllByDeletedAtIsNull(Pageable pageable);

    Optional<Dog> findByIdAndDeletedAtIsNull(Long id);
}
