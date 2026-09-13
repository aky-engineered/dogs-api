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
    void seedsStatuses() {
        List<String> statuses = jdbcTemplate.queryForList("SELECT name FROM dog_status", String.class);

        assertThat(statuses).containsExactlyInAnyOrder("In Training", "In Service", "Retired", "Left");
    }

    @Test
    void seedsLeavingReasons() {
        List<String> reasons = jdbcTemplate.queryForList("SELECT name FROM leaving_reason", String.class);

        assertThat(reasons).containsExactlyInAnyOrder(
                "Transferred", "Retired (Put Down)", "KIA", "Rejected", "Retired (Re-housed)", "Died");
    }
}
