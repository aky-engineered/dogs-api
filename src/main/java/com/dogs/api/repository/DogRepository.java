package com.dogs.api.repository;

import com.dogs.api.model.Dog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface DogRepository extends JpaRepository<Dog, Long> {

    @EntityGraph(attributePaths = {"breed", "supplier", "status", "leavingReason"})
    Optional<Dog> findByIdAndDeletedAtIsNull(Long id);

    // A null filter value means "don't filter on this field"
    @EntityGraph(attributePaths = {"breed", "supplier", "status", "leavingReason"})
    @Query("""
            SELECT d FROM Dog d
            LEFT JOIN d.breed b
            LEFT JOIN d.supplier s
            WHERE (:includeDeleted = true OR d.deletedAt IS NULL)
              AND (:name IS NULL OR LOWER(d.name) LIKE LOWER(CONCAT('%', :name, '%')))
              AND (:breed IS NULL OR LOWER(b.name) LIKE LOWER(CONCAT('%', :breed, '%')))
              AND (:supplier IS NULL OR LOWER(s.name) LIKE LOWER(CONCAT('%', :supplier, '%')))
            """)
    Page<Dog> search(@Param("name") String name,
                     @Param("breed") String breed,
                     @Param("supplier") String supplier,
                     @Param("includeDeleted") boolean includeDeleted,
                     Pageable pageable);
}
