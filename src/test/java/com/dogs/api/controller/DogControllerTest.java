package com.dogs.api.controller;

import com.dogs.api.dto.LookupResponse;
import com.dogs.api.dto.DogFilter;
import com.dogs.api.dto.DogRequest;
import com.dogs.api.dto.DogResponse;
import com.dogs.api.dto.PageResponse;
import com.dogs.api.exception.ResourceNotFoundException;
import com.dogs.api.model.Gender;
import com.dogs.api.service.DogService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
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

@WebMvcTest(DogController.class)
class DogControllerTest {

    private static final LookupResponse IN_SERVICE = new LookupResponse(2L, "IN_SERVICE", "In Service");

    private static final List<DogResponse> SIX_DOGS = List.of(
            dog(1L, "Rex"),
            dog(2L, "Bella"),
            dog(3L, "Scout"),
            dog(4L, "Digby"),
            dog(5L, "Max"),
            dog(6L, "Luna"));

    private static final String VALID_REQUEST = """
            {
              "name": "Rex",
              "breedId": 1,
              "supplierId": 3,
              "badgeId": "PD1001",
              "gender": "MALE",
              "birthDate": "2020-04-12",
              "dateAcquired": "2021-01-05",
              "currentStatus": "IN_SERVICE",
              "kennellingCharacteristic": "Reactive to other males"
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DogService dogService;

    @Test
    void getAllDogs_WithMoreThanFiveDogs_ReturnsExpectedRecords() throws Exception {
        when(dogService.getAllDogs(any(DogFilter.class), any(Pageable.class), eq(false)))
                .thenReturn(new PageResponse<>(SIX_DOGS.subList(0, 5), 0, 5, 6, 2));

        mockMvc.perform(get("/api/dogs/dogs").param("page", "0").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(5))
                .andExpect(jsonPath("$.content[0].name").value("Rex"))
                .andExpect(jsonPath("$.content[0].currentStatus.code").value("IN_SERVICE"))
                .andExpect(jsonPath("$.content[4].name").value("Max"))
                .andExpect(jsonPath("$.totalElements").value(6))
                .andExpect(jsonPath("$.totalPages").value(2));

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(dogService).getAllDogs(any(DogFilter.class), pageable.capture(), eq(false));
        assertThat(pageable.getValue().getPageNumber()).isZero();
        assertThat(pageable.getValue().getPageSize()).isEqualTo(5);
    }

    @Test
    void getAllDogs_WithFilterParameter_PassesParsedFilterToService() throws Exception {
        when(dogService.getAllDogs(any(DogFilter.class), any(Pageable.class), eq(false)))
                .thenReturn(new PageResponse<>(List.of(), 0, 20, 0, 0));

        mockMvc.perform(get("/api/dogs/dogs")
                        .param("filter", "{\"name\": \"rex\", \"breed\": \"shepherd\", \"supplier\": \"kennels\"}"))
                .andExpect(status().isOk());

        verify(dogService).getAllDogs(eq(new DogFilter("rex", "shepherd", "kennels")), any(Pageable.class), eq(false));
    }

    @Test
    void getAllDogs_WithMalformedFilter_ReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/dogs/dogs").param("filter", "{name: "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Invalid value for parameter 'filter'"));

        verifyNoInteractions(dogService);
    }

    @Test
    void getDogById_WithExistingId_ReturnsDog() throws Exception {
        when(dogService.getDogById(1L)).thenReturn(dog(1L, "Rex"));

        mockMvc.perform(get("/api/dogs/dogs/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Rex"))
                .andExpect(jsonPath("$.gender").value("MALE"))
                .andExpect(jsonPath("$.birthDate").value("2020-04-12"))
                .andExpect(jsonPath("$.currentStatus.code").value("IN_SERVICE"))
                .andExpect(jsonPath("$.currentStatus.name").value("In Service"));
    }

    @Test
    void getDogById_WithUnknownId_ReturnsNotFound() throws Exception {
        when(dogService.getDogById(99L)).thenThrow(new ResourceNotFoundException("Dog", 99L));

        mockMvc.perform(get("/api/dogs/dogs/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").exists());
    }

    @Test
    void createDog_WithValidRequest_ReturnsCreated() throws Exception {
        when(dogService.createDog(any(DogRequest.class))).thenReturn(dog(1L, "Rex"));

        mockMvc.perform(post("/api/dogs/dogs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REQUEST))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));

        ArgumentCaptor<DogRequest> request = ArgumentCaptor.forClass(DogRequest.class);
        verify(dogService).createDog(request.capture());
        assertThat(request.getValue().name()).isEqualTo("Rex");
        assertThat(request.getValue().currentStatus()).isEqualTo("IN_SERVICE");
        assertThat(request.getValue().gender()).isEqualTo(Gender.MALE);
        assertThat(request.getValue().birthDate()).isEqualTo(LocalDate.of(2020, 4, 12));
    }

    @Test
    void createDog_WithMissingNameAndCurrentStatus_ReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/dogs/dogs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"breedId\": 1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").exists())
                .andExpect(jsonPath("$.errors.currentStatus").exists());

        verifyNoInteractions(dogService);
    }

    @Test
    void updateDogById_WithValidRequest_ReturnsUpdatedDog() throws Exception {
        when(dogService.updateDogById(eq(1L), any(DogRequest.class))).thenReturn(dog(1L, "Rex"));

        mockMvc.perform(put("/api/dogs/dogs/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REQUEST))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Rex"));
    }

    @Test
    void deleteDogById_WithExistingId_ReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/dogs/dogs/1"))
                .andExpect(status().isNoContent());

        verify(dogService).deleteDogById(1L);
    }

    private static DogResponse dog(Long id, String name) {
        return new DogResponse(id, name, null, null, null, Gender.MALE, LocalDate.of(2020, 4, 12), null,
                IN_SERVICE, null, null, null, null, null, null);
    }
}
