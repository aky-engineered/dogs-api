package com.dogs.api.config;

import com.dogs.api.dto.DogFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

@Component
@RequiredArgsConstructor
public class DogFilterConverter implements Converter<String, DogFilter> {

    private final JsonMapper jsonMapper;

    @Override
    public DogFilter convert(final String source) {
        if (source.isBlank()) {
            return DogFilter.empty();
        }
        // Reject unknown keys so a typo like "nmae" fails instead of silently returning every dog
        return jsonMapper.readerFor(DogFilter.class)
                .with(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .readValue(source);
    }
}
