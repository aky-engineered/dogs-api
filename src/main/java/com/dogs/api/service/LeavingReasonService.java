package com.dogs.api.service;

import com.dogs.api.dto.LookupRequest;
import com.dogs.api.dto.LookupResponse;
import com.dogs.api.dto.PageResponse;
import com.dogs.api.exception.ResourceNotFoundException;
import com.dogs.api.mapper.LookupMapper;
import com.dogs.api.model.LeavingReason;
import com.dogs.api.repository.LeavingReasonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class LeavingReasonService {

    private final LeavingReasonRepository leavingReasonRepository;
    private final LookupMapper lookupMapper;

    @Transactional(readOnly = true)
    public PageResponse<LookupResponse> getAllLeavingReasons(final Pageable pageable, final boolean includeDeleted) {
        Page<LeavingReason> leavingReasons = includeDeleted
                ? leavingReasonRepository.findAll(pageable)
                : leavingReasonRepository.findAllByDeletedAtIsNull(pageable);
        return PageResponse.from(leavingReasons.map(lookupMapper::toResponse));
    }

    @Transactional(readOnly = true)
    public LookupResponse getLeavingReasonById(final Long id) {
        return lookupMapper.toResponse(findActive(id));
    }

    @Transactional
    public LookupResponse createLeavingReason(final LookupRequest request) {
        return lookupMapper.toResponse(leavingReasonRepository.saveAndFlush(lookupMapper.toLeavingReason(request)));
    }

    @Transactional
    public LookupResponse updateLeavingReasonById(final Long id, final LookupRequest request) {
        LeavingReason leavingReason = findActive(id);
        lookupMapper.update(request, leavingReason);
        return lookupMapper.toResponse(leavingReasonRepository.saveAndFlush(leavingReason));
    }

    @Transactional
    public void deleteLeavingReasonById(final Long id) {
        findActive(id).setDeletedAt(Instant.now());
    }

    private LeavingReason findActive(final Long id) {
        return leavingReasonRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Leaving reason", id));
    }
}
