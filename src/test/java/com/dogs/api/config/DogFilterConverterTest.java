package com.dogs.api.config;

import com.dogs.api.dto.DogFilter;
import org.junit.jupiter.api.Test;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DogFilterConverterTest {

    private final DogFilterConverter converter = new DogFilterConverter(JsonMapper.builder().build());

    @Test
    void convert_WithAllFields_ReturnsPopulatedFilter() {
        DogFilter filter = converter.convert("""
                {"name": "rex", "breed": "shepherd", "supplier": "kennels"}
                """);

        assertThat(filter).isEqualTo(new DogFilter("rex", "shepherd", "kennels"));
    }

    @Test
    void convert_WithMalformedJson_ThrowsJacksonException() {
        assertThatThrownBy(() -> converter.convert("{name: ")).isInstanceOf(JacksonException.class);
    }

    @Test
    void convert_WithUnknownKey_ThrowsJacksonException() {
        assertThatThrownBy(() -> converter.convert("{\"colour\": \"black\"}")).isInstanceOf(JacksonException.class);
    }
}
