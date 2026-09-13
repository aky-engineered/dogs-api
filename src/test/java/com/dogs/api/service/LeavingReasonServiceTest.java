package com.dogs.api.service;

import com.dogs.api.dto.LookupRequest;
import com.dogs.api.dto.LookupResponse;
import com.dogs.api.dto.PageResponse;
import com.dogs.api.exception.ResourceNotFoundException;
import com.dogs.api.mapper.LookupMapper;
import com.dogs.api.model.LeavingReason;
import com.dogs.api.repository.LeavingReasonRepository;
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
class LeavingReasonServiceTest {

    @Mock
    private LeavingReasonRepository leavingReasonRepository;

    private LeavingReasonService leavingReasonService;

    @BeforeEach
    void setUp() {
        leavingReasonService = new LeavingReasonService(leavingReasonRepository, Mappers.getMapper(LookupMapper.class));
    }

    @Test
    void getAllLeavingReasons_WithIncludeDeletedFalse_ReturnsOnlyActiveLeavingReasons() {
        Pageable pageable = PageRequest.of(0, 20);
        when(leavingReasonRepository.findAllByDeletedAtIsNull(pageable))
                .thenReturn(new PageImpl<>(List.of(leavingReason(1L, "Transferred")), pageable, 1));

        PageResponse<LookupResponse> result = leavingReasonService.getAllLeavingReasons(pageable, false);

        assertThat(result.content()).containsExactly(new LookupResponse(1L, "Transferred"));
        assertThat(result.totalElements()).isEqualTo(1);
        verify(leavingReasonRepository, never()).findAll(pageable);
    }

    @Test
    void getAllLeavingReasons_WithIncludeDeletedTrue_QueriesAllLeavingReasons() {
        Pageable pageable = PageRequest.of(0, 20);
        when(leavingReasonRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(), pageable, 0));

        leavingReasonService.getAllLeavingReasons(pageable, true);

        verify(leavingReasonRepository, never()).findAllByDeletedAtIsNull(any());
    }

    @Test
    void getLeavingReasonById_WithUnknownOrDeletedId_ThrowsResourceNotFound() {
        when(leavingReasonRepository.findByIdAndDeletedAtIsNull(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> leavingReasonService.getLeavingReasonById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Leaving reason with id 99 not found");
    }

    @Test
    void createLeavingReason_WithValidRequest_ReturnsSavedLeavingReason() {
        when(leavingReasonRepository.saveAndFlush(any(LeavingReason.class))).thenAnswer(invocation -> {
            LeavingReason saved = invocation.getArgument(0);
            saved.setId(5L);
            return saved;
        });

        LookupResponse result = leavingReasonService.createLeavingReason(new LookupRequest("Retired (Re-housed)"));

        assertThat(result).isEqualTo(new LookupResponse(5L, "Retired (Re-housed)"));
    }

    @Test
    void updateLeavingReasonById_WithExistingId_ReturnsRenamedLeavingReason() {
        LeavingReason existing = leavingReason(3L, "Medical");
        when(leavingReasonRepository.findByIdAndDeletedAtIsNull(3L)).thenReturn(Optional.of(existing));
        when(leavingReasonRepository.saveAndFlush(existing)).thenReturn(existing);

        LookupResponse result = leavingReasonService.updateLeavingReasonById(3L, new LookupRequest("KIA"));

        assertThat(result).isEqualTo(new LookupResponse(3L, "KIA"));
    }

    @Test
    void deleteLeavingReasonById_WithExistingId_SetsDeletedAtWithoutRemovingRecord() {
        LeavingReason existing = leavingReason(3L, "Medical");
        when(leavingReasonRepository.findByIdAndDeletedAtIsNull(3L)).thenReturn(Optional.of(existing));

        leavingReasonService.deleteLeavingReasonById(3L);

        assertThat(existing.getDeletedAt()).isNotNull();
        verify(leavingReasonRepository, never()).delete(any());
        verify(leavingReasonRepository, never()).deleteById(any());
    }

    @Test
    void deleteLeavingReasonById_WithAlreadyDeletedId_ThrowsResourceNotFound() {
        when(leavingReasonRepository.findByIdAndDeletedAtIsNull(3L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> leavingReasonService.deleteLeavingReasonById(3L)).isInstanceOf(ResourceNotFoundException.class);
    }

    private LeavingReason leavingReason(Long id, String name) {
        LeavingReason leavingReason = new LeavingReason();
        leavingReason.setId(id);
        leavingReason.setName(name);
        return leavingReason;
    }
}
