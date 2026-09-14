package com.dogs.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class LiquibaseMigrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void createsAllTables() {
        List<String> tables = jdbcTemplate.queryForList(
                "SELECT table_name FROM information_schema.tables WHERE table_schema = DATABASE()", String.class);

        assertThat(tables).contains("dog", "breed", "supplier", "dog_status", "leaving_reason");
    }

    @Test
    void seededStatuses_WhenMigrationsApplied_HaveCodes() {
        List<String> codes = jdbcTemplate.queryForList(
                "SELECT code FROM dog_status WHERE name IN ('In Training', 'In Service', 'Retired', 'Left')", String.class);

        assertThat(codes).containsExactlyInAnyOrder("IN_TRAINING", "IN_SERVICE", "RETIRED", "LEFT");
    }

    @Test
    void seededLeavingReasons_WhenMigrationsApplied_HaveCodes() {
        List<String> codes = jdbcTemplate.queryForList(
                "SELECT code FROM leaving_reason WHERE name IN "
                        + "('Transferred', 'Retired (Put Down)', 'KIA', 'Rejected', 'Retired (Re-housed)', 'Died')",
                String.class);

        assertThat(codes).containsExactlyInAnyOrder(
                "TRANSFERRED", "RETIRED_PUT_DOWN", "KIA", "REJECTED", "RETIRED_REHOUSED", "DIED");
    }
}
