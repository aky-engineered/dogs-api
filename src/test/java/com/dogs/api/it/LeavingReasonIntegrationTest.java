package com.dogs.api.it;

import com.dogs.api.TestcontainersConfiguration;
import com.dogs.api.repository.LeavingReasonRepository;
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
class LeavingReasonIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LeavingReasonRepository leavingReasonRepository;

    @Test
    void leavingReasonEndpoints_WithFullCrudLifecycle_SoftDeletesAndHidesLeavingReason() throws Exception {
        String name = uniqueName("Retired (Re-housed)");
        long id = createLeavingReason(name);

        mockMvc.perform(get("/api/dogs/leaving-reasons/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(name));

        String renamed = uniqueName("Medically Retired");
        mockMvc.perform(put("/api/dogs/leaving-reasons/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(renamed)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(renamed));

        mockMvc.perform(get("/api/dogs/leaving-reasons").param("size", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].name", hasItem(renamed)));

        mockMvc.perform(delete("/api/dogs/leaving-reasons/{id}", id))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/dogs/leaving-reasons/{id}", id))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/dogs/leaving-reasons").param("size", "100"))
                .andExpect(jsonPath("$.content[*].name", not(hasItem(renamed))));
        mockMvc.perform(get("/api/dogs/leaving-reasons").param("size", "100").param("includeDeleted", "true"))
                .andExpect(jsonPath("$.content[*].name", hasItem(renamed)));

        assertThat(leavingReasonRepository.findById(id)).hasValueSatisfying(leavingReason ->
                assertThat(leavingReason.getDeletedAt()).isNotNull());
    }

    @Test
    void deleteLeavingReasonById_WithAlreadyDeletedLeavingReason_ReturnsNotFound() throws Exception {
        long id = createLeavingReason(uniqueName("Failed Assessment"));

        mockMvc.perform(delete("/api/dogs/leaving-reasons/{id}", id)).andExpect(status().isNoContent());
        mockMvc.perform(delete("/api/dogs/leaving-reasons/{id}", id)).andExpect(status().isNotFound());
    }

    @Test
    void createLeavingReason_WithDuplicateName_ReturnsConflict() throws Exception {
        String name = uniqueName("Relocated");
        createLeavingReason(name);

        mockMvc.perform(post("/api/dogs/leaving-reasons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(name)))
                .andExpect(status().isConflict());
    }

    @Test
    void getAllLeavingReasons_WithPageSizeOne_ReturnsSingleRecordPage() throws Exception {
        createLeavingReason(uniqueName("Unfit For Duty"));
        createLeavingReason(uniqueName("Owner Request"));

        mockMvc.perform(get("/api/dogs/leaving-reasons").param("page", "0").param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.page").value(0));
    }

    @Test
    void getAllLeavingReasons_WithInvalidSortProperty_ReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/dogs/leaving-reasons").param("sort", "notAField"))
                .andExpect(status().isBadRequest());
    }

    private long createLeavingReason(String name) throws Exception {
        String body = mockMvc.perform(post("/api/dogs/leaving-reasons")
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
