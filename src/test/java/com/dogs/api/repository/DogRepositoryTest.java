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
    void defaultsGenderToUnknown() {
        Long id = dogRepository.save(newDog("Bella")).getId();
        flushAndClear();

        assertThat(dogRepository.findById(id).orElseThrow().getGender()).isEqualTo(Gender.UNKNOWN);
    }

    @Test
    void excludesSoftDeletedDogsFromActiveQueries() {
        Dog active = dogRepository.save(newDog("Active"));
        Dog deleted = newDog("Deleted");
        deleted.setDeletedAt(Instant.now());
        dogRepository.save(deleted);
        flushAndClear();

        assertThat(dogRepository.findAllByDeletedAtIsNull(Pageable.unpaged()))
                .extracting(Dog::getName)
                .contains("Active")
                .doesNotContain("Deleted");
        assertThat(dogRepository.findByIdAndDeletedAtIsNull(active.getId())).isPresent();
        assertThat(dogRepository.findByIdAndDeletedAtIsNull(deleted.getId())).isEmpty();
        assertThat(dogRepository.findById(deleted.getId())).isPresent();
    }

    @Test
    void dogStillLoadsWhenItsStatusIsSoftDeleted() {
        DogStatus retiredStatus = new DogStatus();
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
