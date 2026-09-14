package com.dogs.api.it;

import com.dogs.api.TestcontainersConfiguration;
import com.dogs.api.repository.DogRepository;
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
import static org.hamcrest.Matchers.containsInAnyOrder;
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
class DogIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DogRepository dogRepository;

    @Test
    void dogEndpoints_WithFullCrudLifecycle_SoftDeletesAndHidesDog() throws Exception {
        String token = token();
        long breedId = createLookup("/api/dogs/breeds", "{\"name\": \"German Shepherd %s\"}".formatted(token));
        long supplierId = createLookup("/api/dogs/suppliers", "{\"name\": \"Northfield Kennels %s\"}".formatted(token));
        String name = "Rex " + token;

        long id = createDog("""
                {"name": "%s", "breedId": %d, "supplierId": %d, "badgeId": "%s", "gender": "MALE",
                 "birthDate": "2020-04-12", "dateAcquired": "2021-01-05", "currentStatus": "IN_TRAINING",
                 "kennellingCharacteristic": "Reactive to other males"}
                """.formatted(name, breedId, supplierId, token));

        mockMvc.perform(get("/api/dogs/dogs/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(name))
                .andExpect(jsonPath("$.breed.name").value("German Shepherd " + token))
                .andExpect(jsonPath("$.supplier.name").value("Northfield Kennels " + token))
                .andExpect(jsonPath("$.currentStatus.code").value("IN_TRAINING"))
                .andExpect(jsonPath("$.currentStatus.name").value("In Training"))
                .andExpect(jsonPath("$.gender").value("MALE"))
                .andExpect(jsonPath("$.kennellingCharacteristic").value("Reactive to other males"))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.deletedAt").doesNotExist());

        mockMvc.perform(put("/api/dogs/dogs/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "%s", "breedId": %d, "supplierId": %d, "badgeId": "%s", "gender": "MALE",
                                 "currentStatus": "LEFT", "leavingDate": "2024-08-01", "leavingReason": "TRANSFERRED"}
                                """.formatted(name, breedId, supplierId, token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentStatus.code").value("LEFT"))
                .andExpect(jsonPath("$.leavingDate").value("2024-08-01"))
                .andExpect(jsonPath("$.leavingReason.code").value("TRANSFERRED"))
                .andExpect(jsonPath("$.leavingReason.name").value("Transferred"))
                .andExpect(jsonPath("$.kennellingCharacteristic").doesNotExist());

        String nameFilter = "{\"name\": \"%s\"}".formatted(token);
        mockMvc.perform(get("/api/dogs/dogs").param("filter", nameFilter))
                .andExpect(jsonPath("$.content[*].name", hasItem(name)));

        mockMvc.perform(delete("/api/dogs/dogs/{id}", id))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/dogs/dogs/{id}", id))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/dogs/dogs").param("filter", nameFilter))
                .andExpect(jsonPath("$.content[*].name", not(hasItem(name))));
        mockMvc.perform(get("/api/dogs/dogs").param("filter", nameFilter).param("includeDeleted", "true"))
                .andExpect(jsonPath("$.content[*].name", hasItem(name)))
                .andExpect(jsonPath("$.content[0].deletedAt").exists());

        assertThat(dogRepository.findById(id)).hasValueSatisfying(dog ->
                assertThat(dog.getDeletedAt()).isNotNull());
    }

    @Test
    void getAllDogs_WithFilterOnNameBreedAndSupplier_ReturnsOnlyMatchingDogs() throws Exception {
        String token = token();
        long malinois = createLookup("/api/dogs/breeds", "{\"name\": \"Malinois %s\"}".formatted(token));
        long spaniel = createLookup("/api/dogs/breeds", "{\"name\": \"Spaniel %s\"}".formatted(token));
        long northfield = createLookup("/api/dogs/suppliers", "{\"name\": \"Northfield %s\"}".formatted(token));
        long brookvale = createLookup("/api/dogs/suppliers", "{\"name\": \"Brookvale %s\"}".formatted(token));

        createDog(dogJson("Rex " + token, malinois, northfield));
        createDog(dogJson("Bella " + token, malinois, brookvale));
        createDog(dogJson("T-Rex " + token, spaniel, northfield));

        expectFilterMatches("{\"name\": \"rex %s\"}".formatted(token), "Rex " + token, "T-Rex " + token);
        expectFilterMatches("{\"breed\": \"malinois %s\"}".formatted(token), "Rex " + token, "Bella " + token);
        expectFilterMatches("{\"supplier\": \"NORTHFIELD %s\"}".formatted(token), "Rex " + token, "T-Rex " + token);
        expectFilterMatches("{\"name\": \"rex\", \"breed\": \"malinois %s\", \"supplier\": \"northfield %s\"}"
                .formatted(token, token), "Rex " + token);
    }

    private void expectFilterMatches(String filter, String... expectedNames) throws Exception {
        mockMvc.perform(get("/api/dogs/dogs").param("filter", filter).param("size", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].name", containsInAnyOrder(expectedNames)));
    }

    private String dogJson(String name, long breedId, long supplierId) {
        return "{\"name\": \"%s\", \"breedId\": %d, \"supplierId\": %d, \"currentStatus\": \"IN_SERVICE\"}"
                .formatted(name, breedId, supplierId);
    }

    private long createDog(String json) throws Exception {
        return idFrom(mockMvc.perform(post("/api/dogs/dogs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString());
    }

    private long createLookup(String path, String json) throws Exception {
        return idFrom(mockMvc.perform(post(path)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString());
    }

    private long idFrom(String body) {
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private String token() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
