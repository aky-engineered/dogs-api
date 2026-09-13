package com.dogs.api.controller;

import com.dogs.api.dto.LookupRequest;
import com.dogs.api.dto.LookupResponse;
import com.dogs.api.dto.PageResponse;
import com.dogs.api.service.BreedService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/dogs/breeds", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Tag(name = "Breeds")
public class BreedController {

    private static final Logger log = LoggerFactory.getLogger(BreedController.class);

    private final BreedService breedService;

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public PageResponse<LookupResponse> getAllBreeds(@ParameterObject Pageable pageable,
                                                     @RequestParam(defaultValue = "false") boolean includeDeleted) {
        log.info("Request received for List breeds - page: {}, size: {}, includeDeleted: {}",
                pageable.getPageNumber(), pageable.getPageSize(), includeDeleted);
        return breedService.getAllBreeds(pageable, includeDeleted);
    }

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public LookupResponse getBreedById(@PathVariable Long id) {
        log.info("Request received for Get breed by id: {}", id);
        return breedService.getBreedById(id);
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public LookupResponse createBreed(@Valid @RequestBody LookupRequest request) {
        log.info("Request received for Create breed with name: {}", request.name());
        return breedService.createBreed(request);
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public LookupResponse updateBreedById(@PathVariable Long id, @Valid @RequestBody LookupRequest request) {
        log.info("Request received for Update breed by id: {} with name: {}", id, request.name());
        return breedService.updateBreedById(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteBreedById(@PathVariable Long id) {
        log.info("Request received for Delete breed by id: {}", id);
        breedService.deleteBreedById(id);
    }
}
