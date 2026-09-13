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
import static org.mockito.Mockito.doThrow;
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
            new LookupResponse(1L, "Transferred"),
            new LookupResponse(2L, "KIA"),
            new LookupResponse(3L, "Rejected"),
            new LookupResponse(4L, "Died"),
            new LookupResponse(5L, "Retired (Put Down)"),
            new LookupResponse(6L, "Retired (Re-housed)"));

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
                .andExpect(jsonPath("$.content[0].name").value("Transferred"))
                .andExpect(jsonPath("$.content[4].name").value("Retired (Put Down)"))
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
    void getAllLeavingReasons_RequestingSecondPageOfSixLeavingReasons_ReturnsRemainingRecord() throws Exception {
        when(leavingReasonService.getAllLeavingReasons(any(Pageable.class), eq(false)))
                .thenReturn(new PageResponse<>(SIX_LEAVING_REASONS.subList(5, 6), 1, 5, 6, 2));

        mockMvc.perform(get("/api/dogs/leaving-reasons").param("page", "1").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Retired (Re-housed)"))
                .andExpect(jsonPath("$.page").value(1));

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(leavingReasonService).getAllLeavingReasons(pageable.capture(), eq(false));
        assertThat(pageable.getValue().getPageNumber()).isEqualTo(1);
    }

    @Test
    void getAllLeavingReasons_WithIncludeDeletedTrue_PassesFlagToService() throws Exception {
        when(leavingReasonService.getAllLeavingReasons(any(Pageable.class), eq(true)))
                .thenReturn(new PageResponse<>(List.of(), 0, 20, 0, 0));

        mockMvc.perform(get("/api/dogs/leaving-reasons").param("includeDeleted", "true"))
                .andExpect(status().isOk());

        verify(leavingReasonService).getAllLeavingReasons(any(Pageable.class), eq(true));
    }

    @Test
    void getLeavingReasonById_WithExistingId_ReturnsLeavingReason() throws Exception {
        when(leavingReasonService.getLeavingReasonById(1L)).thenReturn(new LookupResponse(1L, "Transferred"));

        mockMvc.perform(get("/api/dogs/leaving-reasons/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
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
        when(leavingReasonService.createLeavingReason(new LookupRequest("Retired (Re-housed)"))).thenReturn(new LookupResponse(5L, "Retired (Re-housed)"));

        mockMvc.perform(post("/api/dogs/leaving-reasons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Retired (Re-housed)"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.name").value("Retired (Re-housed)"));
    }

    @Test
    void createLeavingReason_WithBlankName_ReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/dogs/leaving-reasons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": " "}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").exists());

        verifyNoInteractions(leavingReasonService);
    }

    @Test
    void updateLeavingReasonById_WithValidRequest_ReturnsUpdatedLeavingReason() throws Exception {
        when(leavingReasonService.updateLeavingReasonById(1L, new LookupRequest("KIA")))
                .thenReturn(new LookupResponse(1L, "KIA"));

        mockMvc.perform(put("/api/dogs/leaving-reasons/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "KIA"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("KIA"));
    }

    @Test
    void updateLeavingReasonById_WithUnknownId_ReturnsNotFound() throws Exception {
        when(leavingReasonService.updateLeavingReasonById(99L, new LookupRequest("KIA")))
                .thenThrow(new ResourceNotFoundException("Leaving reason", 99L));

        mockMvc.perform(put("/api/dogs/leaving-reasons/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "KIA"}
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteLeavingReasonById_WithExistingId_ReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/dogs/leaving-reasons/1"))
                .andExpect(status().isNoContent());

        verify(leavingReasonService).deleteLeavingReasonById(1L);
    }

    @Test
    void deleteLeavingReasonById_WithUnknownId_ReturnsNotFound() throws Exception {
        doThrow(new ResourceNotFoundException("Leaving reason", 99L)).when(leavingReasonService).deleteLeavingReasonById(99L);

        mockMvc.perform(delete("/api/dogs/leaving-reasons/99"))
                .andExpect(status().isNotFound());
    }
}
