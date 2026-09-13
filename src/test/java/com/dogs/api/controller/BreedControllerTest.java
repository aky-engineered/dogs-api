package com.dogs.api.controller;

import com.dogs.api.dto.LookupRequest;
import com.dogs.api.dto.LookupResponse;
import com.dogs.api.dto.PageResponse;
import com.dogs.api.exception.ResourceNotFoundException;
import com.dogs.api.service.BreedService;
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

@WebMvcTest(BreedController.class)
class BreedControllerTest {

    private static final List<LookupResponse> SIX_BREEDS = List.of(
            new LookupResponse(1L, "Labrador"),
            new LookupResponse(2L, "German Shepherd"),
            new LookupResponse(3L, "Belgian Malinois"),
            new LookupResponse(4L, "Springer Spaniel"),
            new LookupResponse(5L, "Cocker Spaniel"),
            new LookupResponse(6L, "Beagle"));

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BreedService breedService;

    @Test
    void getAllBreeds_WithMoreThanFiveBreeds_ReturnsExpectedRecords() throws Exception {
        when(breedService.getAllBreeds(any(Pageable.class), eq(false)))
                .thenReturn(new PageResponse<>(SIX_BREEDS.subList(0, 5), 0, 5, 6, 2));

        mockMvc.perform(get("/api/dogs/breeds").param("page", "0").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(5))
                .andExpect(jsonPath("$.content[0].name").value("Labrador"))
                .andExpect(jsonPath("$.content[4].name").value("Cocker Spaniel"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(5))
                .andExpect(jsonPath("$.totalElements").value(6))
                .andExpect(jsonPath("$.totalPages").value(2));

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(breedService).getAllBreeds(pageable.capture(), eq(false));
        assertThat(pageable.getValue().getPageNumber()).isZero();
        assertThat(pageable.getValue().getPageSize()).isEqualTo(5);
    }

    @Test
    void getAllBreeds_RequestingSecondPageOfSixBreeds_ReturnsRemainingRecord() throws Exception {
        when(breedService.getAllBreeds(any(Pageable.class), eq(false)))
                .thenReturn(new PageResponse<>(SIX_BREEDS.subList(5, 6), 1, 5, 6, 2));

        mockMvc.perform(get("/api/dogs/breeds").param("page", "1").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Beagle"))
                .andExpect(jsonPath("$.page").value(1));

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(breedService).getAllBreeds(pageable.capture(), eq(false));
        assertThat(pageable.getValue().getPageNumber()).isEqualTo(1);
    }

    @Test
    void getAllBreeds_WithIncludeDeletedTrue_PassesFlagToService() throws Exception {
        when(breedService.getAllBreeds(any(Pageable.class), eq(true)))
                .thenReturn(new PageResponse<>(List.of(), 0, 20, 0, 0));

        mockMvc.perform(get("/api/dogs/breeds").param("includeDeleted", "true"))
                .andExpect(status().isOk());

        verify(breedService).getAllBreeds(any(Pageable.class), eq(true));
    }

    @Test
    void getBreedById_WithExistingId_ReturnsBreed() throws Exception {
        when(breedService.getBreedById(1L)).thenReturn(new LookupResponse(1L, "Labrador"));

        mockMvc.perform(get("/api/dogs/breeds/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Labrador"));
    }

    @Test
    void getBreedById_WithUnknownId_ReturnsNotFound() throws Exception {
        when(breedService.getBreedById(99L)).thenThrow(new ResourceNotFoundException("Breed", 99L));

        mockMvc.perform(get("/api/dogs/breeds/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").exists());
    }

    @Test
    void createBreed_WithValidRequest_ReturnsCreated() throws Exception {
        when(breedService.createBreed(new LookupRequest("Beagle"))).thenReturn(new LookupResponse(5L, "Beagle"));

        mockMvc.perform(post("/api/dogs/breeds")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Beagle"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.name").value("Beagle"));
    }

    @Test
    void createBreed_WithBlankName_ReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/dogs/breeds")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": " "}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").exists());

        verifyNoInteractions(breedService);
    }

    @Test
    void updateBreedById_WithValidRequest_ReturnsUpdatedBreed() throws Exception {
        when(breedService.updateBreedById(1L, new LookupRequest("German Shepherd")))
                .thenReturn(new LookupResponse(1L, "German Shepherd"));

        mockMvc.perform(put("/api/dogs/breeds/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "German Shepherd"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("German Shepherd"));
    }

    @Test
    void updateBreedById_WithUnknownId_ReturnsNotFound() throws Exception {
        when(breedService.updateBreedById(99L, new LookupRequest("German Shepherd")))
                .thenThrow(new ResourceNotFoundException("Breed", 99L));

        mockMvc.perform(put("/api/dogs/breeds/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "German Shepherd"}
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteBreedById_WithExistingId_ReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/dogs/breeds/1"))
                .andExpect(status().isNoContent());

        verify(breedService).deleteBreedById(1L);
    }

    @Test
    void deleteBreedById_WithUnknownId_ReturnsNotFound() throws Exception {
        doThrow(new ResourceNotFoundException("Breed", 99L)).when(breedService).deleteBreedById(99L);

        mockMvc.perform(delete("/api/dogs/breeds/99"))
                .andExpect(status().isNotFound());
    }
}
