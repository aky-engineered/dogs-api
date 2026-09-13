package com.dogs.api.repository;

import com.dogs.api.TestcontainersConfiguration;
import com.dogs.api.model.Breed;
import com.dogs.api.model.LookupEntity;
import com.dogs.api.model.Supplier;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class LookupRepositoryTest {

    @Autowired
    private BreedRepository breedRepository;

    @Autowired
    private DogStatusRepository dogStatusRepository;

    @Autowired
    private LeavingReasonRepository leavingReasonRepository;

    @Autowired
    private SupplierRepository supplierRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void loadsSeededStatusesAndLeavingReasons() {
        assertThat(dogStatusRepository.findAllByDeletedAtIsNull(Pageable.unpaged()))
                .extracting(LookupEntity::getName)
                .contains("In Training", "In Service", "Retired", "Left");
        assertThat(leavingReasonRepository.findAllByDeletedAtIsNull(Pageable.unpaged()))
                .extracting(LookupEntity::getName)
                .contains("Transferred", "KIA", "Died");
    }

    @Test
    void excludesSoftDeletedLookupValues() {
        Breed activeBreed = breedRepository.save(breed("Labrador Retriever"));
        Breed deletedBreed = breed("Alsatian");
        deletedBreed.setDeletedAt(LocalDate.now());
        breedRepository.save(deletedBreed);
        entityManager.flush();
        entityManager.clear();

        assertThat(breedRepository.findAllByDeletedAtIsNull(Pageable.unpaged()))
                .extracting(LookupEntity::getName)
                .contains("Labrador Retriever")
                .doesNotContain("Alsatian");
        assertThat(breedRepository.findByIdAndDeletedAtIsNull(activeBreed.getId())).isPresent();
        assertThat(breedRepository.findByIdAndDeletedAtIsNull(deletedBreed.getId())).isEmpty();
    }

    @Test
    void rejectsDuplicateNames() {
        supplierRepository.saveAndFlush(supplier("Random Kennels"));

        assertThatThrownBy(() -> supplierRepository.saveAndFlush(supplier("Random Kennels")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private Breed breed(final String name) {
        Breed breed = new Breed();
        breed.setName(name);
        return breed;
    }

    private Supplier supplier(final String name) {
        Supplier supplier = new Supplier();
        supplier.setName(name);
        return supplier;
    }
}
