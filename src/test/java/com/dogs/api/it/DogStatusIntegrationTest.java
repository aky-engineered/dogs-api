package com.dogs.api.it;

import com.dogs.api.TestcontainersConfiguration;
import com.dogs.api.repository.DogStatusRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class DogStatusIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DogStatusRepository dogStatusRepository;

    @Test
    void dogStatusEndpoints_WithFullCrudLifecycle_SoftDeletesAndHidesDogStatus() throws Exception {
        String name = uniqueName("On Loan");
        long id = createDogStatus(name);

        mockMvc.perform(get("/api/dogs/statuses/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(name));

        String renamed = uniqueName("Operational");
        mockMvc.perform(put("/api/dogs/statuses/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(renamed)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(renamed));

        mockMvc.perform(get("/api/dogs/statuses").param("size", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].name", hasItem(renamed)));

        mockMvc.perform(delete("/api/dogs/statuses/{id}", id))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/dogs/statuses/{id}", id))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/dogs/statuses").param("size", "100"))
                .andExpect(jsonPath("$.content[*].name", not(hasItem(renamed))));
        mockMvc.perform(get("/api/dogs/statuses").param("size", "100").param("includeDeleted", "true"))
                .andExpect(jsonPath("$.content[*].name", hasItem(renamed)));

        assertThat(dogStatusRepository.findById(id)).hasValueSatisfying(dogStatus ->
                assertThat(dogStatus.getDeletedAt()).isNotNull());
    }

    @Test
    void deleteDogStatusById_WithAlreadyDeletedDogStatus_ReturnsNotFound() throws Exception {
        long id = createDogStatus(uniqueName("Seconded"));

        mockMvc.perform(delete("/api/dogs/statuses/{id}", id)).andExpect(status().isNoContent());
        mockMvc.perform(delete("/api/dogs/statuses/{id}", id)).andExpect(status().isNotFound());
    }

    @Test
    void createDogStatus_WithDuplicateName_ReturnsConflict() throws Exception {
        String name = uniqueName("Under Review");
        createDogStatus(name);

        mockMvc.perform(post("/api/dogs/statuses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(name)))
                .andExpect(status().isConflict());
    }

    @Test
    void getAllDogStatuses_WithPageSizeOne_ReturnsSingleRecordPage() throws Exception {
        createDogStatus(uniqueName("Probation"));
        createDogStatus(uniqueName("Reserve"));

        mockMvc.perform(get("/api/dogs/statuses").param("page", "0").param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.page").value(0));
    }

    @Test
    void getAllDogStatuses_WithInvalidSortProperty_ReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/dogs/statuses").param("sort", "notAField"))
                .andExpect(status().isBadRequest());
    }

    private long createDogStatus(String name) throws Exception {
        String body = mockMvc.perform(post("/api/dogs/statuses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(name)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private String json(String name) {
        return "{\"name\": \"%s\"}".formatted(name);
    }

    private String uniqueName(String prefix) {
        return prefix + " " + UUID.randomUUID().toString().substring(0, 8);
    }
}
