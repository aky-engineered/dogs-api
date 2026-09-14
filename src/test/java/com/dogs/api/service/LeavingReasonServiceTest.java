package com.dogs.api.service;

import com.dogs.api.dto.LookupRequest;
import com.dogs.api.dto.LookupResponse;
import com.dogs.api.dto.PageResponse;
import com.dogs.api.exception.InvalidRequestException;
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
                .thenReturn(new PageImpl<>(List.of(leavingReason(1L, "TRANSFERRED", "Transferred")), pageable, 1));

        PageResponse<LookupResponse> result = leavingReasonService.getAllLeavingReasons(pageable, false);

        assertThat(result.content()).containsExactly(new LookupResponse(1L, "TRANSFERRED", "Transferred"));
        assertThat(result.totalElements()).isEqualTo(1);
        verify(leavingReasonRepository, never()).findAll(pageable);
    }

    @Test
    void getLeavingReasonById_WithUnknownOrDeletedId_ThrowsResourceNotFound() {
        when(leavingReasonRepository.findByIdAndDeletedAtIsNull(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> leavingReasonService.getLeavingReasonById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Leaving reason with id 99 not found");
    }

    @Test
    void createLeavingReason_WithValidRequest_ReturnsSavedLeavingReasonWithCode() {
        when(leavingReasonRepository.saveAndFlush(any(LeavingReason.class))).thenAnswer(invocation -> {
            LeavingReason saved = invocation.getArgument(0);
            saved.setId(5L);
            return saved;
        });

        LookupResponse result = leavingReasonService.createLeavingReason(
                new LookupRequest("RETIRED_REHOUSED", "Retired (Re-housed)"));

        assertThat(result).isEqualTo(new LookupResponse(5L, "RETIRED_REHOUSED", "Retired (Re-housed)"));
    }

    @Test
    void updateLeavingReasonById_WithExistingId_RenamesWithoutChangingCode() {
        LeavingReason existing = leavingReason(3L, "TRANSFERRED", "Transferred");
        when(leavingReasonRepository.findByIdAndDeletedAtIsNull(3L)).thenReturn(Optional.of(existing));
        when(leavingReasonRepository.saveAndFlush(existing)).thenReturn(existing);

        LookupResponse result = leavingReasonService.updateLeavingReasonById(3L, new LookupRequest(null, "Transfer"));

        assertThat(result).isEqualTo(new LookupResponse(3L, "TRANSFERRED", "Transfer"));
    }

    @Test
    void deleteLeavingReasonById_WithExistingId_SetsDeletedAtWithoutRemovingRecord() {
        LeavingReason existing = leavingReason(3L, "TRANSFERRED", "Transferred");
        when(leavingReasonRepository.findByIdAndDeletedAtIsNull(3L)).thenReturn(Optional.of(existing));

        leavingReasonService.deleteLeavingReasonById(3L);

        assertThat(existing.getDeletedAt()).isNotNull();
        verify(leavingReasonRepository, never()).delete(any());
        verify(leavingReasonRepository, never()).deleteById(any());
    }

    @Test
    void createLeavingReason_WithoutCode_ThrowsInvalidRequest() {
        assertThatThrownBy(() -> leavingReasonService.createLeavingReason(new LookupRequest(null, "Medical")))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("code is required for a leaving reason");
        verify(leavingReasonRepository, never()).saveAndFlush(any());
    }

    private LeavingReason leavingReason(Long id, String code, String name) {
        LeavingReason leavingReason = new LeavingReason();
        leavingReason.setId(id);
        leavingReason.setCode(code);
        leavingReason.setName(name);
        return leavingReason;
    }
}
