package com.dogs.api.controller;

import com.dogs.api.dto.LookupRequest;
import com.dogs.api.dto.LookupResponse;
import com.dogs.api.dto.PageResponse;
import com.dogs.api.service.LeavingReasonService;
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
@RequestMapping(value = "/api/dogs/leaving-reasons", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Tag(name = "Leaving Reasons")
public class LeavingReasonController {

    private static final Logger log = LoggerFactory.getLogger(LeavingReasonController.class);

    private final LeavingReasonService leavingReasonService;

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public PageResponse<LookupResponse> getAllLeavingReasons(@ParameterObject Pageable pageable,
                                                             @RequestParam(defaultValue = "false") boolean includeDeleted) {
        log.info("Request received for List leaving reasons - page: {}, size: {}, includeDeleted: {}",
                pageable.getPageNumber(), pageable.getPageSize(), includeDeleted);
        return leavingReasonService.getAllLeavingReasons(pageable, includeDeleted);
    }

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public LookupResponse getLeavingReasonById(@PathVariable Long id) {
        log.info("Request received for Get leaving reason by id: {}", id);
        return leavingReasonService.getLeavingReasonById(id);
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public LookupResponse createLeavingReason(@Valid @RequestBody LookupRequest request) {
        log.info("Request received for Create leaving reason with name: {}", request.name());
        return leavingReasonService.createLeavingReason(request);
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public LookupResponse updateLeavingReasonById(@PathVariable Long id, @Valid @RequestBody LookupRequest request) {
        log.info("Request received for Update leaving reason by id: {} with name: {}", id, request.name());
        return leavingReasonService.updateLeavingReasonById(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteLeavingReasonById(@PathVariable Long id) {
        log.info("Request received for Delete leaving reason by id: {}", id);
        leavingReasonService.deleteLeavingReasonById(id);
    }
}
