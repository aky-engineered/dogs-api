package com.dogs.api.repository;

import com.dogs.api.model.BaseEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.Optional;

@NoRepositoryBean
public interface LookupRepository<T extends BaseEntity> extends JpaRepository<T, Long> {

    Page<T> findAllByDeletedAtIsNull(Pageable pageable);

    Optional<T> findByIdAndDeletedAtIsNull(Long id);
}
