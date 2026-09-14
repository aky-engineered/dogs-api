package com.dogs.api.controller;

import com.dogs.api.dto.DogFilter;
import com.dogs.api.dto.DogRequest;
import com.dogs.api.dto.DogResponse;
import com.dogs.api.dto.PageResponse;
import com.dogs.api.service.DogService;
import io.swagger.v3.oas.annotations.Parameter;
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
@RequestMapping(value = "/api/dogs/dogs", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Tag(name = "Dogs")
public class DogController {

    private static final Logger log = LoggerFactory.getLogger(DogController.class);

    private final DogService dogService;

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public PageResponse<DogResponse> getAllDogs(
            @Parameter(description = "JSON filter, e.g. {\"name\": \"rex\", \"breed\": \"shepherd\", \"supplier\": \"kennels\"}")
            @RequestParam(required = false) DogFilter filter,
            @ParameterObject Pageable pageable,
            @RequestParam(defaultValue = "false") boolean includeDeleted) {
        DogFilter dogFilter = filter == null ? DogFilter.empty() : filter;
        log.info("Request received for List dogs - filter: {}, page: {}, size: {}, includeDeleted: {}",
                dogFilter, pageable.getPageNumber(), pageable.getPageSize(), includeDeleted);
        return dogService.getAllDogs(dogFilter, pageable, includeDeleted);
    }

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public DogResponse getDogById(@PathVariable Long id) {
        log.info("Request received for Get dog by id: {}", id);
        return dogService.getDogById(id);
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public DogResponse createDog(@Valid @RequestBody DogRequest request) {
        log.info("Request received for Create dog with name: {}", request.name());
        return dogService.createDog(request);
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public DogResponse updateDogById(@PathVariable Long id, @Valid @RequestBody DogRequest request) {
        log.info("Request received for Update dog by id: {} with name: {}", id, request.name());
        return dogService.updateDogById(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDogById(@PathVariable Long id) {
        log.info("Request received for Delete dog by id: {}", id);
        dogService.deleteDogById(id);
    }
}
