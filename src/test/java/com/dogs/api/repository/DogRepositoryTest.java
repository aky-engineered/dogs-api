package com.dogs.api.repository;

import com.dogs.api.TestcontainersConfiguration;
import com.dogs.api.model.Breed;
import com.dogs.api.model.Dog;
import com.dogs.api.model.DogStatus;
import com.dogs.api.model.Gender;
import com.dogs.api.model.Supplier;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class DogRepositoryTest {

    @Autowired
    private DogRepository dogRepository;

    @Autowired
    private DogStatusRepository dogStatusRepository;

    @Autowired
    private EntityManager entityManager;

    private DogStatus inService;

    @BeforeEach
    void setUp() {
        inService = dogStatusRepository.findAll().stream()
                .filter(status -> status.getName().equals("In Service"))
                .findFirst()
                .orElseThrow();
    }

    @Test
    void savesDogWithRelationshipsAndTimestamps() {
        Breed breed = new Breed();
        breed.setName("German Shepherd");
        entityManager.persist(breed);

        Supplier supplier = new Supplier();
        supplier.setName("Northfield Kennels");
        entityManager.persist(supplier);

        Dog dog = newDog("Rex");
        dog.setBreed(breed);
        dog.setSupplier(supplier);
        dog.setBadgeId("PD1001");
        dog.setGender(Gender.MALE);
        dog.setBirthDate(LocalDate.of(2020, 4, 12));
        dog.setKennellingCharacteristic("Reactive to other males");
        Long id = dogRepository.save(dog).getId();
        flushAndClear();

        Dog found = dogRepository.findById(id).orElseThrow();

        assertThat(found.getName()).isEqualTo("Rex");
        assertThat(found.getBreed().getName()).isEqualTo("German Shepherd");
        assertThat(found.getSupplier().getName()).isEqualTo("Northfield Kennels");
        assertThat(found.getStatus().getName()).isEqualTo("In Service");
        assertThat(found.getGender()).isEqualTo(Gender.MALE);
        assertThat(found.getBirthDate()).isEqualTo(LocalDate.of(2020, 4, 12));
        assertThat(found.getCreatedAt()).isNotNull();
        assertThat(found.getUpdatedAt()).isNotNull();
        assertThat(found.getDeletedAt()).isNull();
    }

    @Test
    void dogStillLoadsWhenItsStatusIsSoftDeleted() {
        DogStatus retiredStatus = new DogStatus();
        retiredStatus.setCode("SECONDED");
        retiredStatus.setName("Seconded");
        entityManager.persist(retiredStatus);

        Dog dog = newDog("Scout");
        dog.setStatus(retiredStatus);
        Long id = dogRepository.save(dog).getId();

        retiredStatus.setDeletedAt(Instant.now());
        flushAndClear();

        Dog found = dogRepository.findByIdAndDeletedAtIsNull(id).orElseThrow();
        assertThat(found.getStatus().getName()).isEqualTo("Seconded");
    }

    @Test
    void search_WithNameFilter_ReturnsCaseInsensitivePartialMatches() {
        givenSearchDogs();

        assertThat(searchNames("REX", null, null, false)).containsExactlyInAnyOrder("Rex", "T-Rex");
    }

    @Test
    void search_WithBreedFilter_ReturnsDogsOfMatchingBreed() {
        givenSearchDogs();

        assertThat(searchNames(null, "shepherd", null, false)).containsExactlyInAnyOrder("Rex", "Bella");
    }

    @Test
    void search_WithSupplierFilter_ReturnsDogsFromMatchingSupplier() {
        givenSearchDogs();

        assertThat(searchNames(null, null, "brookvale", false)).containsExactlyInAnyOrder("T-Rex", "Bella");
    }

    @Test
    void search_WithIncludeDeletedFalse_ExcludesDeletedDogs() {
        givenSearchDogs();
        softDelete("Bella");

        assertThat(searchNames(null, "shepherd", null, false)).containsExactly("Rex");
    }

    private void givenSearchDogs() {
        Breed germanShepherd = persistBreed("German Shepherd");
        Breed springerSpaniel = persistBreed("Springer Spaniel");
        Supplier northfieldKennels = persistSupplier("Northfield Kennels");
        Supplier brookvaleBreeders = persistSupplier("Brookvale Breeders");

        saveDog("Rex", germanShepherd, northfieldKennels);
        saveDog("T-Rex", springerSpaniel, brookvaleBreeders);
        saveDog("Bella", germanShepherd, brookvaleBreeders);
        saveDog("Scout", null, null);
        flushAndClear();
    }

    private List<String> searchNames(String name, String breed, String supplier, boolean includeDeleted) {
        return dogRepository.search(name, breed, supplier, includeDeleted, Pageable.unpaged())
                .map(Dog::getName)
                .getContent();
    }

    private void softDelete(String name) {
        dogRepository.findAll().stream()
                .filter(dog -> dog.getName().equals(name))
                .forEach(dog -> dog.setDeletedAt(Instant.now()));
        flushAndClear();
    }

    private Breed persistBreed(String name) {
        Breed breed = new Breed();
        breed.setName(name);
        entityManager.persist(breed);
        return breed;
    }

    private Supplier persistSupplier(String name) {
        Supplier supplier = new Supplier();
        supplier.setName(name);
        entityManager.persist(supplier);
        return supplier;
    }

    private void saveDog(String name, Breed breed, Supplier supplier) {
        Dog dog = newDog(name);
        dog.setBreed(breed);
        dog.setSupplier(supplier);
        dogRepository.save(dog);
    }

    private Dog newDog(String name) {
        Dog dog = new Dog();
        dog.setName(name);
        dog.setStatus(inService);
        return dog;
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}
