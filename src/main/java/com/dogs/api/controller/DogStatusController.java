package com.dogs.api.controller;

import com.dogs.api.dto.LookupRequest;
import com.dogs.api.dto.LookupResponse;
import com.dogs.api.dto.PageResponse;
import com.dogs.api.service.DogStatusService;
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
@RequestMapping(value = "/api/dogs/statuses", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Tag(name = "Statuses")
public class DogStatusController {

    private static final Logger log = LoggerFactory.getLogger(DogStatusController.class);

    private final DogStatusService dogStatusService;

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public PageResponse<LookupResponse> getAllDogStatuses(
            @ParameterObject Pageable pageable,
            @RequestParam(defaultValue = "false") boolean includeDeleted) {
        log.info("Request received for List statuses - page: {}, size: {}, includeDeleted: {}",
                pageable.getPageNumber(), pageable.getPageSize(), includeDeleted);
        return dogStatusService.getAllDogStatuses(pageable, includeDeleted);
    }

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public LookupResponse getDogStatusById(@PathVariable Long id) {
        log.info("Request received for Get status by id: {}", id);
        return dogStatusService.getDogStatusById(id);
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public LookupResponse createDogStatus(@Valid @RequestBody LookupRequest request) {
        log.info("Request received for Create status with code: {} and name: {}", request.code(), request.name());
        return dogStatusService.createDogStatus(request);
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public LookupResponse updateDogStatusById(@PathVariable Long id,
                                                   @Valid @RequestBody LookupRequest request) {
        log.info("Request received for Update status by id: {} with name: {}", id, request.name());
        return dogStatusService.updateDogStatusById(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDogStatusById(@PathVariable Long id) {
        log.info("Request received for Delete status by id: {}", id);
        dogStatusService.deleteDogStatusById(id);
    }
}
