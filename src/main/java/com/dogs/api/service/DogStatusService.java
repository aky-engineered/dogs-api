package com.dogs.api.service;

import com.dogs.api.dto.LookupRequest;
import com.dogs.api.dto.LookupResponse;
import com.dogs.api.dto.PageResponse;
import com.dogs.api.exception.ResourceNotFoundException;
import com.dogs.api.mapper.LookupMapper;
import com.dogs.api.model.DogStatus;
import com.dogs.api.repository.DogStatusRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class DogStatusService {

    private final DogStatusRepository dogStatusRepository;
    private final LookupMapper lookupMapper;

    @Transactional(readOnly = true)
    public PageResponse<LookupResponse> getAllDogStatuses(final Pageable pageable, final boolean includeDeleted) {
        Page<DogStatus> dogStatuses = includeDeleted
                ? dogStatusRepository.findAll(pageable)
                : dogStatusRepository.findAllByDeletedAtIsNull(pageable);
        return PageResponse.from(dogStatuses.map(lookupMapper::toResponse));
    }

    @Transactional(readOnly = true)
    public LookupResponse getDogStatusById(final Long id) {
        return lookupMapper.toResponse(findActive(id));
    }

    @Transactional
    public LookupResponse createDogStatus(final LookupRequest request) {
        return lookupMapper.toResponse(dogStatusRepository.saveAndFlush(lookupMapper.toDogStatus(request)));
    }

    @Transactional
    public LookupResponse updateDogStatusById(final Long id, final LookupRequest request) {
        DogStatus dogStatus = findActive(id);
        lookupMapper.update(request, dogStatus);
        return lookupMapper.toResponse(dogStatusRepository.saveAndFlush(dogStatus));
    }

    @Transactional
    public void deleteDogStatusById(final Long id) {
        findActive(id).setDeletedAt(Instant.now());
    }

    private DogStatus findActive(final Long id) {
        return dogStatusRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Status", id));
    }
}
