package com.dogs.api.service;

import com.dogs.api.dto.LookupRequest;
import com.dogs.api.dto.LookupResponse;
import com.dogs.api.dto.PageResponse;
import com.dogs.api.exception.InvalidRequestException;
import com.dogs.api.exception.ResourceNotFoundException;
import com.dogs.api.mapper.LookupMapper;
import com.dogs.api.model.Breed;
import com.dogs.api.repository.BreedRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class BreedService {

    private final BreedRepository breedRepository;
    private final LookupMapper lookupMapper;

    @Transactional(readOnly = true)
    public PageResponse<LookupResponse> getAllBreeds(final Pageable pageable, final boolean includeDeleted) {
        Page<Breed> breeds = includeDeleted
                ? breedRepository.findAll(pageable)
                : breedRepository.findAllByDeletedAtIsNull(pageable);
        return PageResponse.from(breeds.map(lookupMapper::toResponse));
    }

    @Transactional(readOnly = true)
    public LookupResponse getBreedById(final Long id) {
        return lookupMapper.toResponse(findActive(id));
    }

    @Transactional
    public LookupResponse createBreed(final LookupRequest request) {
        rejectCode(request);
        return lookupMapper.toResponse(breedRepository.saveAndFlush(lookupMapper.toBreed(request)));
    }

    @Transactional
    public LookupResponse updateBreedById(final Long id, final LookupRequest request) {
        rejectCode(request);
        Breed breed = findActive(id);
        lookupMapper.update(request, breed);
        return lookupMapper.toResponse(breedRepository.saveAndFlush(breed));
    }

    @Transactional
    public void deleteBreedById(final Long id) {
        findActive(id).setDeletedAt(Instant.now());
    }

    private static void rejectCode(final LookupRequest request) {
        if (request.code() != null) {
            throw new InvalidRequestException("Breeds do not have a code");
        }
    }

    private Breed findActive(final Long id) {
        return breedRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Breed", id));
    }
}
