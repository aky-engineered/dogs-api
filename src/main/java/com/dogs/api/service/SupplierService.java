package com.dogs.api.service;

import com.dogs.api.dto.LookupRequest;
import com.dogs.api.dto.LookupResponse;
import com.dogs.api.dto.PageResponse;
import com.dogs.api.exception.InvalidRequestException;
import com.dogs.api.exception.ResourceNotFoundException;
import com.dogs.api.mapper.LookupMapper;
import com.dogs.api.model.Supplier;
import com.dogs.api.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class SupplierService {

    private final SupplierRepository supplierRepository;
    private final LookupMapper lookupMapper;

    @Transactional(readOnly = true)
    public PageResponse<LookupResponse> getAllSuppliers(final Pageable pageable, final boolean includeDeleted) {
        Page<Supplier> suppliers = includeDeleted
                ? supplierRepository.findAll(pageable)
                : supplierRepository.findAllByDeletedAtIsNull(pageable);
        return PageResponse.from(suppliers.map(lookupMapper::toResponse));
    }

    @Transactional(readOnly = true)
    public LookupResponse getSupplierById(final Long id) {
        return lookupMapper.toResponse(findActive(id));
    }

    @Transactional
    public LookupResponse createSupplier(final LookupRequest request) {
        rejectCode(request);
        return lookupMapper.toResponse(supplierRepository.saveAndFlush(lookupMapper.toSupplier(request)));
    }

    @Transactional
    public LookupResponse updateSupplierById(final Long id, final LookupRequest request) {
        rejectCode(request);
        Supplier supplier = findActive(id);
        lookupMapper.update(request, supplier);
        return lookupMapper.toResponse(supplierRepository.saveAndFlush(supplier));
    }

    @Transactional
    public void deleteSupplierById(final Long id) {
        findActive(id).setDeletedAt(Instant.now());
    }

    private static void rejectCode(final LookupRequest request) {
        if (request.code() != null) {
            throw new InvalidRequestException("Suppliers do not have a code");
        }
    }

    private Supplier findActive(final Long id) {
        return supplierRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier", id));
    }
}
