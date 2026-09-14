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
        String code = uniqueCode("RETIRED_REHOUSED");
        String name = uniqueName("Retired (Re-housed)");
        long id = createLeavingReason(code, name);

        mockMvc.perform(get("/api/dogs/leaving-reasons/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(code))
                .andExpect(jsonPath("$.name").value(name));

        String renamed = uniqueName("Medically Discharged");
        mockMvc.perform(put("/api/dogs/leaving-reasons/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"%s\"}".formatted(renamed)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(code))
                .andExpect(jsonPath("$.name").value(renamed));

        mockMvc.perform(get("/api/dogs/leaving-reasons").param("size", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].code", hasItem(code)));

        mockMvc.perform(delete("/api/dogs/leaving-reasons/{id}", id))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/dogs/leaving-reasons/{id}", id))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/dogs/leaving-reasons").param("size", "100"))
                .andExpect(jsonPath("$.content[*].code", not(hasItem(code))));
        mockMvc.perform(get("/api/dogs/leaving-reasons").param("size", "100").param("includeDeleted", "true"))
                .andExpect(jsonPath("$.content[*].code", hasItem(code)));

        assertThat(leavingReasonRepository.findById(id)).hasValueSatisfying(leavingReason ->
                assertThat(leavingReason.getDeletedAt()).isNotNull());
    }

    private long createLeavingReason(String code, String name) throws Exception {
        String body = mockMvc.perform(post("/api/dogs/leaving-reasons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(code, name)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private String json(String code, String name) {
        return "{\"code\": \"%s\", \"name\": \"%s\"}".formatted(code, name);
    }

    private String uniqueName(String prefix) {
        return prefix + " " + UUID.randomUUID().toString().substring(0, 8);
    }

    private String uniqueCode(String prefix) {
        return prefix + "_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
