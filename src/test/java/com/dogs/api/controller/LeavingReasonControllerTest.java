package com.dogs.api.controller;

import com.dogs.api.dto.LookupRequest;
import com.dogs.api.dto.LookupResponse;
import com.dogs.api.dto.PageResponse;
import com.dogs.api.exception.ResourceNotFoundException;
import com.dogs.api.service.LeavingReasonService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LeavingReasonController.class)
class LeavingReasonControllerTest {

    private static final List<LookupResponse> SIX_LEAVING_REASONS = List.of(
            new LookupResponse(1L, "TRANSFERRED", "Transferred"),
            new LookupResponse(2L, "KIA", "Killed In Action"),
            new LookupResponse(3L, "REJECTED", "Rejected"),
            new LookupResponse(4L, "DIED", "Died"),
            new LookupResponse(5L, "RETIRED_PUT_DOWN", "Retired (Put Down)"),
            new LookupResponse(6L, "RETIRED_REHOUSED", "Retired (Re-housed)"));

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LeavingReasonService leavingReasonService;

    @Test
    void getAllLeavingReasons_WithMoreThanFiveLeavingReasons_ReturnsExpectedRecords() throws Exception {
        when(leavingReasonService.getAllLeavingReasons(any(Pageable.class), eq(false)))
                .thenReturn(new PageResponse<>(SIX_LEAVING_REASONS.subList(0, 5), 0, 5, 6, 2));

        mockMvc.perform(get("/api/dogs/leaving-reasons").param("page", "0").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(5))
                .andExpect(jsonPath("$.content[0].code").value("TRANSFERRED"))
                .andExpect(jsonPath("$.content[0].name").value("Transferred"))
                .andExpect(jsonPath("$.content[4].code").value("RETIRED_PUT_DOWN"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(5))
                .andExpect(jsonPath("$.totalElements").value(6))
                .andExpect(jsonPath("$.totalPages").value(2));

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(leavingReasonService).getAllLeavingReasons(pageable.capture(), eq(false));
        assertThat(pageable.getValue().getPageNumber()).isZero();
        assertThat(pageable.getValue().getPageSize()).isEqualTo(5);
    }

    @Test
    void getLeavingReasonById_WithExistingId_ReturnsLeavingReason() throws Exception {
        when(leavingReasonService.getLeavingReasonById(1L)).thenReturn(new LookupResponse(1L, "TRANSFERRED", "Transferred"));

        mockMvc.perform(get("/api/dogs/leaving-reasons/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.code").value("TRANSFERRED"))
                .andExpect(jsonPath("$.name").value("Transferred"));
    }

    @Test
    void getLeavingReasonById_WithUnknownId_ReturnsNotFound() throws Exception {
        when(leavingReasonService.getLeavingReasonById(99L)).thenThrow(new ResourceNotFoundException("Leaving reason", 99L));

        mockMvc.perform(get("/api/dogs/leaving-reasons/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").exists());
    }

    @Test
    void createLeavingReason_WithValidRequest_ReturnsCreated() throws Exception {
        when(leavingReasonService.createLeavingReason(new LookupRequest("RETIRED_REHOUSED", "Retired (Re-housed)")))
                .thenReturn(new LookupResponse(6L, "RETIRED_REHOUSED", "Retired (Re-housed)"));

        mockMvc.perform(post("/api/dogs/leaving-reasons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code": "RETIRED_REHOUSED", "name": "Retired (Re-housed)"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(6))
                .andExpect(jsonPath("$.code").value("RETIRED_REHOUSED"))
                .andExpect(jsonPath("$.name").value("Retired (Re-housed)"));
    }

    @Test
    void createLeavingReason_WithBlankNameAndCode_ReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/dogs/leaving-reasons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code": "", "name": " "}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").exists())
                .andExpect(jsonPath("$.errors.code").exists());

        verifyNoInteractions(leavingReasonService);
    }

    @Test
    void updateLeavingReasonById_WithValidRequest_ReturnsUpdatedLeavingReason() throws Exception {
        when(leavingReasonService.updateLeavingReasonById(1L, new LookupRequest(null, "Transfer")))
                .thenReturn(new LookupResponse(1L, "TRANSFERRED", "Transfer"));

        mockMvc.perform(put("/api/dogs/leaving-reasons/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Transfer"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("TRANSFERRED"))
                .andExpect(jsonPath("$.name").value("Transfer"));
    }

    @Test
    void deleteLeavingReasonById_WithExistingId_ReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/dogs/leaving-reasons/1"))
                .andExpect(status().isNoContent());

        verify(leavingReasonService).deleteLeavingReasonById(1L);
    }
}
