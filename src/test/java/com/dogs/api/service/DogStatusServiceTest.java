package com.dogs.api.service;

import com.dogs.api.dto.LookupRequest;
import com.dogs.api.dto.LookupResponse;
import com.dogs.api.dto.PageResponse;
import com.dogs.api.exception.InvalidRequestException;
import com.dogs.api.exception.ResourceNotFoundException;
import com.dogs.api.mapper.LookupMapper;
import com.dogs.api.model.DogStatus;
import com.dogs.api.repository.DogStatusRepository;
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
class DogStatusServiceTest {

    @Mock
    private DogStatusRepository dogStatusRepository;

    private DogStatusService dogStatusService;

    @BeforeEach
    void setUp() {
        dogStatusService = new DogStatusService(dogStatusRepository, Mappers.getMapper(LookupMapper.class));
    }

    @Test
    void getAllDogStatuses_WithIncludeDeletedFalse_ReturnsOnlyActiveDogStatuses() {
        Pageable pageable = PageRequest.of(0, 20);
        when(dogStatusRepository.findAllByDeletedAtIsNull(pageable))
                .thenReturn(new PageImpl<>(List.of(dogStatus(1L, "IN_TRAINING", "In Training")), pageable, 1));

        PageResponse<LookupResponse> result = dogStatusService.getAllDogStatuses(pageable, false);

        assertThat(result.content()).containsExactly(new LookupResponse(1L, "IN_TRAINING", "In Training"));
        assertThat(result.totalElements()).isEqualTo(1);
        verify(dogStatusRepository, never()).findAll(pageable);
    }

    @Test
    void getDogStatusById_WithUnknownOrDeletedId_ThrowsResourceNotFound() {
        when(dogStatusRepository.findByIdAndDeletedAtIsNull(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> dogStatusService.getDogStatusById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Status with id 99 not found");
    }

    @Test
    void createDogStatus_WithValidRequest_ReturnsSavedDogStatusWithCode() {
        when(dogStatusRepository.saveAndFlush(any(DogStatus.class))).thenAnswer(invocation -> {
            DogStatus saved = invocation.getArgument(0);
            saved.setId(5L);
            return saved;
        });

        LookupResponse result = dogStatusService.createDogStatus(new LookupRequest("ON_LOAN", "On Loan"));

        assertThat(result).isEqualTo(new LookupResponse(5L, "ON_LOAN", "On Loan"));
    }

    @Test
    void updateDogStatusById_WithExistingId_RenamesWithoutChangingCode() {
        DogStatus existing = dogStatus(3L, "IN_TRAINING", "In Training");
        when(dogStatusRepository.findByIdAndDeletedAtIsNull(3L)).thenReturn(Optional.of(existing));
        when(dogStatusRepository.saveAndFlush(existing)).thenReturn(existing);

        LookupResponse result = dogStatusService.updateDogStatusById(3L, new LookupRequest(null, "Training"));

        assertThat(result).isEqualTo(new LookupResponse(3L, "IN_TRAINING", "Training"));
    }

    @Test
    void deleteDogStatusById_WithExistingId_SetsDeletedAtWithoutRemovingRecord() {
        DogStatus existing = dogStatus(3L, "IN_TRAINING", "In Training");
        when(dogStatusRepository.findByIdAndDeletedAtIsNull(3L)).thenReturn(Optional.of(existing));

        dogStatusService.deleteDogStatusById(3L);

        assertThat(existing.getDeletedAt()).isNotNull();
        verify(dogStatusRepository, never()).delete(any());
        verify(dogStatusRepository, never()).deleteById(any());
    }

    @Test
    void createDogStatus_WithoutCode_ThrowsInvalidRequest() {
        assertThatThrownBy(() -> dogStatusService.createDogStatus(new LookupRequest(null, "On Loan")))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("code is required for a status");
        verify(dogStatusRepository, never()).saveAndFlush(any());
    }

    private DogStatus dogStatus(Long id, String code, String name) {
        DogStatus dogStatus = new DogStatus();
        dogStatus.setId(id);
        dogStatus.setCode(code);
        dogStatus.setName(name);
        return dogStatus;
    }
}
