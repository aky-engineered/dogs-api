package com.dogs.api.service;

import com.dogs.api.dto.LookupResponse;
import com.dogs.api.dto.DogFilter;
import com.dogs.api.dto.DogRequest;
import com.dogs.api.dto.DogResponse;
import com.dogs.api.dto.PageResponse;
import com.dogs.api.exception.InvalidReferenceException;
import com.dogs.api.exception.InvalidSortException;
import com.dogs.api.exception.ResourceNotFoundException;
import com.dogs.api.mapper.DogMapperImpl;
import com.dogs.api.mapper.LookupMapperImpl;
import com.dogs.api.model.Breed;
import com.dogs.api.model.Dog;
import com.dogs.api.model.DogStatus;
import com.dogs.api.model.Gender;
import com.dogs.api.model.LeavingReason;
import com.dogs.api.repository.BreedRepository;
import com.dogs.api.repository.DogRepository;
import com.dogs.api.repository.DogStatusRepository;
import com.dogs.api.repository.LeavingReasonRepository;
import com.dogs.api.repository.SupplierRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DogServiceTest {

    @Mock
    private DogRepository dogRepository;

    @Mock
    private BreedRepository breedRepository;

    @Mock
    private SupplierRepository supplierRepository;

    @Mock
    private DogStatusRepository dogStatusRepository;

    @Mock
    private LeavingReasonRepository leavingReasonRepository;

    private DogService dogService;

    @BeforeEach
    void setUp() {
        dogService = new DogService(dogRepository, breedRepository, supplierRepository, dogStatusRepository,
                leavingReasonRepository, new DogMapperImpl(new LookupMapperImpl()));
    }

    @Test
    void getAllDogs_WithFilter_ReturnsMappedPage() {
        Pageable pageable = PageRequest.of(0, 20);
        when(dogRepository.search("rex", "shepherd", "kennels", false, pageable))
                .thenReturn(new PageImpl<>(List.of(dog(1L, "Rex", status(2L, "IN_SERVICE", "In Service"))), pageable, 1));

        PageResponse<DogResponse> result =
                dogService.getAllDogs(new DogFilter("rex", "shepherd", "kennels"), pageable, false);

        assertThat(result.totalElements()).isEqualTo(1);
        assertThat(result.content()).singleElement().satisfies(dog -> {
            assertThat(dog.name()).isEqualTo("Rex");
            assertThat(dog.currentStatus()).isEqualTo(new LookupResponse(2L, "IN_SERVICE", "In Service"));
        });
    }

    @Test
    void getAllDogs_WithUnsupportedSortProperty_ThrowsInvalidSort() {
        Pageable pageable = PageRequest.of(0, 20, Sort.by("notAField"));

        assertThatThrownBy(() -> dogService.getAllDogs(DogFilter.empty(), pageable, false))
                .isInstanceOf(InvalidSortException.class)
                .hasMessageStartingWith("Cannot sort by 'notAField'");
        verifyNoInteractions(dogRepository);
    }

    @Test
    void getDogById_WithUnknownOrDeletedId_ThrowsResourceNotFound() {
        when(dogRepository.findByIdAndDeletedAtIsNull(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> dogService.getDogById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Dog with id 99 not found");
    }

    @Test
    void createDog_WithValidReferences_ReturnsSavedDog() {
        when(breedRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(breed(1L, "German Shepherd")));
        when(dogStatusRepository.findByCodeAndDeletedAtIsNull("IN_SERVICE"))
                .thenReturn(Optional.of(status(2L, "IN_SERVICE", "In Service")));
        when(dogRepository.saveAndFlush(any(Dog.class))).thenAnswer(invocation -> {
            Dog saved = invocation.getArgument(0);
            saved.setId(10L);
            return saved;
        });

        DogResponse result = dogService.createDog(request("Rex", 1L, "IN_SERVICE", null, null));

        assertThat(result.id()).isEqualTo(10L);
        assertThat(result.name()).isEqualTo("Rex");
        assertThat(result.breed()).isEqualTo(new LookupResponse(1L, null, "German Shepherd"));
        assertThat(result.currentStatus()).isEqualTo(new LookupResponse(2L, "IN_SERVICE", "In Service"));
        assertThat(result.supplier()).isNull();
        assertThat(result.leavingReason()).isNull();
        assertThat(result.gender()).isEqualTo(Gender.UNKNOWN);
        assertThat(result.birthDate()).isEqualTo(LocalDate.of(2020, 4, 12));
    }

    @Test
    void createDog_WithUnknownOrDeletedStatusCode_ThrowsInvalidReference() {
        when(dogStatusRepository.findByCodeAndDeletedAtIsNull("TRAINING")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> dogService.createDog(request("Rex", null, "TRAINING", null, null)))
                .isInstanceOf(InvalidReferenceException.class)
                .hasMessage("currentStatus TRAINING does not exist or has been deleted");
        verify(dogRepository, never()).saveAndFlush(any());
    }

    @Test
    void updateDogById_WithUnchangedDeletedStatus_KeepsCurrentStatus() {
        DogStatus deletedStatus = status(9L, "SECONDED", "Seconded");
        deletedStatus.setDeletedAt(Instant.now());
        Dog existing = dog(1L, "Rex", deletedStatus);
        when(dogRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(existing));
        when(dogRepository.saveAndFlush(existing)).thenReturn(existing);

        DogResponse result = dogService.updateDogById(1L, request("Rex Renamed", null, "SECONDED", null, Gender.MALE));

        assertThat(result.name()).isEqualTo("Rex Renamed");
        assertThat(result.currentStatus()).isEqualTo(new LookupResponse(9L, "SECONDED", "Seconded"));
        assertThat(result.gender()).isEqualTo(Gender.MALE);
        verify(dogStatusRepository, never()).findByCodeAndDeletedAtIsNull(any());
    }

    @Test
    void deleteDogById_WithExistingId_SetsDeletedAtWithoutRemovingRecord() {
        Dog existing = dog(1L, "Rex", status(2L, "IN_SERVICE", "In Service"));
        when(dogRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(existing));

        dogService.deleteDogById(1L);

        assertThat(existing.getDeletedAt()).isNotNull();
        verify(dogRepository, never()).delete(any(Dog.class));
        verify(dogRepository, never()).deleteById(any());
    }

    private DogRequest request(String name, Long breedId, String currentStatus, String leavingReason, Gender gender) {
        return new DogRequest(name, breedId, null, null, gender, LocalDate.of(2020, 4, 12), null, currentStatus,
                null, leavingReason, null);
    }

    private Dog dog(Long id, String name, DogStatus status) {
        Dog dog = new Dog();
        dog.setId(id);
        dog.setName(name);
        dog.setStatus(status);
        return dog;
    }

    private Breed breed(Long id, String name) {
        Breed breed = new Breed();
        breed.setId(id);
        breed.setName(name);
        return breed;
    }

    private DogStatus status(Long id, String code, String name) {
        DogStatus status = new DogStatus();
        status.setId(id);
        status.setCode(code);
        status.setName(name);
        return status;
    }

    private LeavingReason leavingReason(Long id, String code, String name) {
        LeavingReason leavingReason = new LeavingReason();
        leavingReason.setId(id);
        leavingReason.setCode(code);
        leavingReason.setName(name);
        return leavingReason;
    }
}
