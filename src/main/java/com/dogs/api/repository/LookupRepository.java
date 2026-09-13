package com.dogs.api.repository;

import com.dogs.api.model.LookupEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.Optional;

@NoRepositoryBean
public interface LookupRepository<T extends LookupEntity> extends JpaRepository<T, Long> {

    Page<T> findAllByDeletedAtIsNull(Pageable pageable);

    Optional<T> findByIdAndDeletedAtIsNull(Long id);
}
