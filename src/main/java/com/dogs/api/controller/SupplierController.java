package com.dogs.api.controller;

import com.dogs.api.dto.LookupRequest;
import com.dogs.api.dto.LookupResponse;
import com.dogs.api.dto.PageResponse;
import com.dogs.api.service.SupplierService;
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
@RequestMapping(value = "/api/dogs/suppliers", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Tag(name = "Suppliers")
public class SupplierController {

    private static final Logger log = LoggerFactory.getLogger(SupplierController.class);

    private final SupplierService supplierService;

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public PageResponse<LookupResponse> getAllSuppliers(@ParameterObject Pageable pageable,
                                                        @RequestParam(defaultValue = "false") boolean includeDeleted) {
        log.info("Request received for List suppliers - page: {}, size: {}, includeDeleted: {}",
                pageable.getPageNumber(), pageable.getPageSize(), includeDeleted);
        return supplierService.getAllSuppliers(pageable, includeDeleted);
    }

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public LookupResponse getSupplierById(@PathVariable Long id) {
        log.info("Request received for Get supplier by id: {}", id);
        return supplierService.getSupplierById(id);
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public LookupResponse createSupplier(@Valid @RequestBody LookupRequest request) {
        log.info("Request received for Create supplier with name: {}", request.name());
        return supplierService.createSupplier(request);
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public LookupResponse updateSupplierById(@PathVariable Long id, @Valid @RequestBody LookupRequest request) {
        log.info("Request received for Update supplier by id: {} with name: {}", id, request.name());
        return supplierService.updateSupplierById(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSupplierById(@PathVariable Long id) {
        log.info("Request received for Delete supplier by id: {}", id);
        supplierService.deleteSupplierById(id);
    }
}
