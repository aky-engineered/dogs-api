package com.dogs.api.service;

import com.dogs.api.dto.DogFilter;
import com.dogs.api.dto.DogRequest;
import com.dogs.api.dto.DogResponse;
import com.dogs.api.dto.PageResponse;
import com.dogs.api.exception.InvalidReferenceException;
import com.dogs.api.exception.InvalidSortException;
import com.dogs.api.exception.ResourceNotFoundException;
import com.dogs.api.mapper.DogMapper;
import com.dogs.api.model.BaseEntity;
import com.dogs.api.model.Dog;
import com.dogs.api.model.DogStatus;
import com.dogs.api.model.LeavingReason;
import com.dogs.api.repository.BreedRepository;
import com.dogs.api.repository.DogRepository;
import com.dogs.api.repository.DogStatusRepository;
import com.dogs.api.repository.LeavingReasonRepository;
import com.dogs.api.repository.LookupRepository;
import com.dogs.api.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DogService {

    private final DogRepository dogRepository;
    private final BreedRepository breedRepository;
    private final SupplierRepository supplierRepository;
    private final DogStatusRepository dogStatusRepository;
    private final LeavingReasonRepository leavingReasonRepository;
    private final DogMapper dogMapper;

    // The search is a hand-written @Query, so Spring Data doesn't validate sort properties for us
    private static final List<String> SORTABLE_FIELDS = List.of(
            "id", "name", "badgeId", "gender", "birthDate", "dateAcquired", "leavingDate", "createdAt", "updatedAt");

    @Transactional(readOnly = true)
    public PageResponse<DogResponse> getAllDogs(final DogFilter filter, final Pageable pageable,
                                                final boolean includeDeleted) {
        pageable.getSort().forEach(order -> {
            if (!SORTABLE_FIELDS.contains(order.getProperty())) {
                throw new InvalidSortException(order.getProperty(), SORTABLE_FIELDS);
            }
        });
        return PageResponse.from(dogRepository
                .search(blankToNull(filter.name()), blankToNull(filter.breed()), blankToNull(filter.supplier()),
                        includeDeleted, pageable)
                .map(dogMapper::toResponse));
    }

    @Transactional(readOnly = true)
    public DogResponse getDogById(final Long id) {
        return dogMapper.toResponse(findActive(id));
    }

    @Transactional
    public DogResponse createDog(final DogRequest request) {
        Dog dog = new Dog();
        apply(request, dog);
        return dogMapper.toResponse(dogRepository.saveAndFlush(dog));
    }

    @Transactional
    public DogResponse updateDogById(final Long id, final DogRequest request) {
        Dog dog = findActive(id);
        apply(request, dog);
        return dogMapper.toResponse(dogRepository.saveAndFlush(dog));
    }

    @Transactional
    public void deleteDogById(final Long id) {
        findActive(id).setDeletedAt(Instant.now());
    }

    private void apply(final DogRequest request, final Dog dog) {
        dog.setBreed(resolve(breedRepository, request.breedId(), dog.getBreed(), "breedId"));
        dog.setSupplier(resolve(supplierRepository, request.supplierId(), dog.getSupplier(), "supplierId"));
        dog.setStatus(resolveStatus(request.currentStatus(), dog.getStatus()));
        dog.setLeavingReason(resolveLeavingReason(request.leavingReason(), dog.getLeavingReason()));
        dogMapper.update(request, dog);
    }

    // A deleted lookup value can't be newly assigned, but a dog that already has it keeps it on update
    private <T extends BaseEntity> T resolve(final LookupRepository<T> repository, final Long id,
                                             final T current, final String field) {
        if (id == null) {
            return null;
        }
        if (current != null && id.equals(current.getId())) {
            return current;
        }
        return repository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new InvalidReferenceException(field, id));
    }

    private DogStatus resolveStatus(final String code, final DogStatus current) {
        String trimmedCode = blankToNull(code);
        if (trimmedCode == null) {
            return null;
        }
        if (current != null && trimmedCode.equalsIgnoreCase(current.getCode())) {
            return current;
        }
        return dogStatusRepository.findByCodeAndDeletedAtIsNull(trimmedCode)
                .orElseThrow(() -> new InvalidReferenceException("currentStatus", trimmedCode));
    }

    private LeavingReason resolveLeavingReason(final String code, final LeavingReason current) {
        String trimmedCode = blankToNull(code);
        if (trimmedCode == null) {
            return null;
        }
        if (current != null && trimmedCode.equalsIgnoreCase(current.getCode())) {
            return current;
        }
        return leavingReasonRepository.findByCodeAndDeletedAtIsNull(trimmedCode)
                .orElseThrow(() -> new InvalidReferenceException("leavingReason", trimmedCode));
    }

    private static String blankToNull(final String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private Dog findActive(final Long id) {
        return dogRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Dog", id));
    }
}
