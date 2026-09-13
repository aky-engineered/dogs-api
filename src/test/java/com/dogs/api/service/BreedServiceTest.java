package com.dogs.api.service;

import com.dogs.api.dto.LookupRequest;
import com.dogs.api.dto.LookupResponse;
import com.dogs.api.dto.PageResponse;
import com.dogs.api.exception.ResourceNotFoundException;
import com.dogs.api.mapper.LookupMapper;
import com.dogs.api.model.Breed;
import com.dogs.api.repository.BreedRepository;
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
class BreedServiceTest {

    @Mock
    private BreedRepository breedRepository;

    private BreedService breedService;

    @BeforeEach
    void setUp() {
        breedService = new BreedService(breedRepository, Mappers.getMapper(LookupMapper.class));
    }

    @Test
    void getAllBreeds_WithIncludeDeletedFalse_ReturnsOnlyActiveBreeds() {
        Pageable pageable = PageRequest.of(0, 20);
        when(breedRepository.findAllByDeletedAtIsNull(pageable))
                .thenReturn(new PageImpl<>(List.of(breed(1L, "Labrador")), pageable, 1));

        PageResponse<LookupResponse> result = breedService.getAllBreeds(pageable, false);

        assertThat(result.content()).containsExactly(new LookupResponse(1L, "Labrador"));
        assertThat(result.totalElements()).isEqualTo(1);
        verify(breedRepository, never()).findAll(pageable);
    }

    @Test
    void getAllBreeds_WithIncludeDeletedTrue_QueriesAllBreeds() {
        Pageable pageable = PageRequest.of(0, 20);
        when(breedRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(), pageable, 0));

        breedService.getAllBreeds(pageable, true);

        verify(breedRepository, never()).findAllByDeletedAtIsNull(any());
    }

    @Test
    void getBreedById_WithUnknownOrDeletedId_ThrowsResourceNotFound() {
        when(breedRepository.findByIdAndDeletedAtIsNull(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> breedService.getBreedById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Breed with id 99 not found");
    }

    @Test
    void createBreed_WithValidRequest_ReturnsSavedBreed() {
        when(breedRepository.saveAndFlush(any(Breed.class))).thenAnswer(invocation -> {
            Breed saved = invocation.getArgument(0);
            saved.setId(5L);
            return saved;
        });

        LookupResponse result = breedService.createBreed(new LookupRequest("Beagle"));

        assertThat(result).isEqualTo(new LookupResponse(5L, "Beagle"));
    }

    @Test
    void updateBreedById_WithExistingId_ReturnsRenamedBreed() {
        Breed existing = breed(3L, "Alsatian");
        when(breedRepository.findByIdAndDeletedAtIsNull(3L)).thenReturn(Optional.of(existing));
        when(breedRepository.saveAndFlush(existing)).thenReturn(existing);

        LookupResponse result = breedService.updateBreedById(3L, new LookupRequest("German Shepherd"));

        assertThat(result).isEqualTo(new LookupResponse(3L, "German Shepherd"));
    }

    @Test
    void deleteBreedById_WithExistingId_SetsDeletedAtWithoutRemovingRecord() {
        Breed existing = breed(3L, "Alsatian");
        when(breedRepository.findByIdAndDeletedAtIsNull(3L)).thenReturn(Optional.of(existing));

        breedService.deleteBreedById(3L);

        assertThat(existing.getDeletedAt()).isNotNull();
        verify(breedRepository, never()).delete(any());
        verify(breedRepository, never()).deleteById(any());
    }

    @Test
    void deleteBreedById_WithAlreadyDeletedId_ThrowsResourceNotFound() {
        when(breedRepository.findByIdAndDeletedAtIsNull(3L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> breedService.deleteBreedById(3L)).isInstanceOf(ResourceNotFoundException.class);
    }

    private Breed breed(Long id, String name) {
        Breed breed = new Breed();
        breed.setId(id);
        breed.setName(name);
        return breed;
    }
}
