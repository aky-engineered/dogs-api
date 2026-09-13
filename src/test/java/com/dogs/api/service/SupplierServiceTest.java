package com.dogs.api.service;

import com.dogs.api.dto.LookupRequest;
import com.dogs.api.dto.LookupResponse;
import com.dogs.api.dto.PageResponse;
import com.dogs.api.exception.ResourceNotFoundException;
import com.dogs.api.mapper.LookupMapper;
import com.dogs.api.model.Supplier;
import com.dogs.api.repository.SupplierRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SupplierServiceTest {

    @Mock
    private SupplierRepository supplierRepository;

    private SupplierService supplierService;

    @BeforeEach
    void setUp() {
        supplierService = new SupplierService(supplierRepository, Mappers.getMapper(LookupMapper.class));
    }

    @Test
    void getAllSuppliers_WithIncludeDeletedFalse_ReturnsOnlyActiveSuppliers() {
        Pageable pageable = PageRequest.of(0, 20);
        when(supplierRepository.findAllByDeletedAtIsNull(pageable))
                .thenReturn(new PageImpl<>(List.of(supplier(1L, "Northfield Kennels")), pageable, 1));

        PageResponse<LookupResponse> result = supplierService.getAllSuppliers(pageable, false);

        assertThat(result.content()).containsExactly(new LookupResponse(1L, "Northfield Kennels"));
        assertThat(result.totalElements()).isEqualTo(1);
        verify(supplierRepository, never()).findAll(pageable);
    }

    @Test
    void getAllSuppliers_WithIncludeDeletedTrue_QueriesAllSuppliers() {
        Pageable pageable = PageRequest.of(0, 20);
        when(supplierRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(), pageable, 0));

        supplierService.getAllSuppliers(pageable, true);

        verify(supplierRepository, never()).findAllByDeletedAtIsNull(any());
    }

    @Test
    void getSupplierById_WithUnknownOrDeletedId_ThrowsResourceNotFound() {
        when(supplierRepository.findByIdAndDeletedAtIsNull(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> supplierService.getSupplierById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Supplier with id 99 not found");
    }

    @Test
    void createSupplier_WithValidRequest_ReturnsSavedSupplier() {
        when(supplierRepository.saveAndFlush(any(Supplier.class))).thenAnswer(invocation -> {
            Supplier saved = invocation.getArgument(0);
            saved.setId(5L);
            return saved;
        });

        LookupResponse result = supplierService.createSupplier(new LookupRequest("Oakridge Kennels"));

        assertThat(result).isEqualTo(new LookupResponse(5L, "Oakridge Kennels"));
    }

    @Test
    void updateSupplierById_WithExistingId_ReturnsRenamedSupplier() {
        Supplier existing = supplier(3L, "Old Mill Kennels");
        when(supplierRepository.findByIdAndDeletedAtIsNull(3L)).thenReturn(Optional.of(existing));
        when(supplierRepository.saveAndFlush(existing)).thenReturn(existing);

        LookupResponse result = supplierService.updateSupplierById(3L, new LookupRequest("Brookvale Kennels"));

        assertThat(result).isEqualTo(new LookupResponse(3L, "Brookvale Kennels"));
    }

    @Test
    void deleteSupplierById_WithExistingId_SetsDeletedAtWithoutRemovingRecord() {
        Supplier existing = supplier(3L, "Old Mill Kennels");
        when(supplierRepository.findByIdAndDeletedAtIsNull(3L)).thenReturn(Optional.of(existing));

        supplierService.deleteSupplierById(3L);

        assertThat(existing.getDeletedAt()).isNotNull();
        verify(supplierRepository, never()).delete(any());
        verify(supplierRepository, never()).deleteById(any());
    }

    @Test
    void deleteSupplierById_WithAlreadyDeletedId_ThrowsResourceNotFound() {
        when(supplierRepository.findByIdAndDeletedAtIsNull(3L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> supplierService.deleteSupplierById(3L)).isInstanceOf(ResourceNotFoundException.class);
    }

    private Supplier supplier(Long id, String name) {
        Supplier supplier = new Supplier();
        supplier.setId(id);
        supplier.setName(name);
        return supplier;
    }
}
