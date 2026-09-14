package com.dogs.api.it;

import com.dogs.api.TestcontainersConfiguration;
import com.dogs.api.repository.BreedRepository;
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
class BreedIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BreedRepository breedRepository;

    @Test
    void breedEndpoints_WithFullCrudLifecycle_SoftDeletesAndHidesBreed() throws Exception {
        String name = uniqueName("Beagle");
        long id = createBreed(name);

        mockMvc.perform(get("/api/dogs/breeds/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(name));

        String renamed = uniqueName("Basset Hound");
        mockMvc.perform(put("/api/dogs/breeds/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(renamed)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(renamed));

        mockMvc.perform(get("/api/dogs/breeds").param("size", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].name", hasItem(renamed)));

        mockMvc.perform(delete("/api/dogs/breeds/{id}", id))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/dogs/breeds/{id}", id))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/dogs/breeds").param("size", "100"))
                .andExpect(jsonPath("$.content[*].name", not(hasItem(renamed))));
        mockMvc.perform(get("/api/dogs/breeds").param("size", "100").param("includeDeleted", "true"))
                .andExpect(jsonPath("$.content[*].name", hasItem(renamed)));

        assertThat(breedRepository.findById(id)).hasValueSatisfying(breed ->
                assertThat(breed.getDeletedAt()).isNotNull());
    }

    private long createBreed(String name) throws Exception {
        String body = mockMvc.perform(post("/api/dogs/breeds")
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
