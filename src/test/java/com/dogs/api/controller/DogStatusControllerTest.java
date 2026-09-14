package com.dogs.api.controller;

import com.dogs.api.dto.LookupRequest;
import com.dogs.api.dto.LookupResponse;
import com.dogs.api.dto.PageResponse;
import com.dogs.api.exception.ResourceNotFoundException;
import com.dogs.api.service.DogStatusService;
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

@WebMvcTest(DogStatusController.class)
class DogStatusControllerTest {

    private static final List<LookupResponse> SIX_STATUSES = List.of(
            new LookupResponse(1L, "IN_TRAINING", "In Training"),
            new LookupResponse(2L, "IN_SERVICE", "In Service"),
            new LookupResponse(3L, "RETIRED", "Retired"),
            new LookupResponse(4L, "LEFT", "Left"),
            new LookupResponse(5L, "SUSPENDED", "Suspended"),
            new LookupResponse(6L, "ON_LOAN", "On Loan"));

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DogStatusService dogStatusService;

    @Test
    void getAllDogStatuses_WithMoreThanFiveDogStatuses_ReturnsExpectedRecords() throws Exception {
        when(dogStatusService.getAllDogStatuses(any(Pageable.class), eq(false)))
                .thenReturn(new PageResponse<>(SIX_STATUSES.subList(0, 5), 0, 5, 6, 2));

        mockMvc.perform(get("/api/dogs/statuses").param("page", "0").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(5))
                .andExpect(jsonPath("$.content[0].code").value("IN_TRAINING"))
                .andExpect(jsonPath("$.content[0].name").value("In Training"))
                .andExpect(jsonPath("$.content[4].code").value("SUSPENDED"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(5))
                .andExpect(jsonPath("$.totalElements").value(6))
                .andExpect(jsonPath("$.totalPages").value(2));

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(dogStatusService).getAllDogStatuses(pageable.capture(), eq(false));
        assertThat(pageable.getValue().getPageNumber()).isZero();
        assertThat(pageable.getValue().getPageSize()).isEqualTo(5);
    }

    @Test
    void getDogStatusById_WithExistingId_ReturnsDogStatus() throws Exception {
        when(dogStatusService.getDogStatusById(1L)).thenReturn(new LookupResponse(1L, "IN_TRAINING", "In Training"));

        mockMvc.perform(get("/api/dogs/statuses/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.code").value("IN_TRAINING"))
                .andExpect(jsonPath("$.name").value("In Training"));
    }

    @Test
    void getDogStatusById_WithUnknownId_ReturnsNotFound() throws Exception {
        when(dogStatusService.getDogStatusById(99L)).thenThrow(new ResourceNotFoundException("Status", 99L));

        mockMvc.perform(get("/api/dogs/statuses/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").exists());
    }

    @Test
    void createDogStatus_WithValidRequest_ReturnsCreated() throws Exception {
        when(dogStatusService.createDogStatus(new LookupRequest("ON_LOAN", "On Loan")))
                .thenReturn(new LookupResponse(6L, "ON_LOAN", "On Loan"));

        mockMvc.perform(post("/api/dogs/statuses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code": "ON_LOAN", "name": "On Loan"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(6))
                .andExpect(jsonPath("$.code").value("ON_LOAN"))
                .andExpect(jsonPath("$.name").value("On Loan"));
    }

    @Test
    void createDogStatus_WithBlankNameAndCode_ReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/dogs/statuses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code": "", "name": " "}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").exists())
                .andExpect(jsonPath("$.errors.code").exists());

        verifyNoInteractions(dogStatusService);
    }

    @Test
    void updateDogStatusById_WithValidRequest_ReturnsUpdatedDogStatus() throws Exception {
        when(dogStatusService.updateDogStatusById(1L, new LookupRequest(null, "Training")))
                .thenReturn(new LookupResponse(1L, "IN_TRAINING", "Training"));

        mockMvc.perform(put("/api/dogs/statuses/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Training"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("IN_TRAINING"))
                .andExpect(jsonPath("$.name").value("Training"));
    }

    @Test
    void deleteDogStatusById_WithExistingId_ReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/dogs/statuses/1"))
                .andExpect(status().isNoContent());

        verify(dogStatusService).deleteDogStatusById(1L);
    }
}
