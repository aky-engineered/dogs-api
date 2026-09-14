package com.dogs.api.controller;

import com.dogs.api.dto.LookupRequest;
import com.dogs.api.dto.LookupResponse;
import com.dogs.api.dto.PageResponse;
import com.dogs.api.exception.ResourceNotFoundException;
import com.dogs.api.service.SupplierService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SupplierController.class)
class SupplierControllerTest {

    private static final List<LookupResponse> SIX_SUPPLIERS = List.of(
            new LookupResponse(1L, null, "Northfield Kennels"),
            new LookupResponse(2L, null, "Brookvale Kennels"),
            new LookupResponse(3L, null, "County Working Dogs"),
            new LookupResponse(4L, null, "Highmoor Kennels"),
            new LookupResponse(5L, null, "Valley Kennels"),
            new LookupResponse(6L, null, "Oakridge Kennels"));

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SupplierService supplierService;

    @Test
    void getAllSuppliers_WithMoreThanFiveSuppliers_ReturnsExpectedRecords() throws Exception {
        when(supplierService.getAllSuppliers(any(Pageable.class), eq(false)))
                .thenReturn(new PageResponse<>(SIX_SUPPLIERS.subList(0, 5), 0, 5, 6, 2));

        mockMvc.perform(get("/api/dogs/suppliers").param("page", "0").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(5))
                .andExpect(jsonPath("$.content[0].name").value("Northfield Kennels"))
                .andExpect(jsonPath("$.content[4].name").value("Valley Kennels"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(5))
                .andExpect(jsonPath("$.totalElements").value(6))
                .andExpect(jsonPath("$.totalPages").value(2));

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(supplierService).getAllSuppliers(pageable.capture(), eq(false));
        assertThat(pageable.getValue().getPageNumber()).isZero();
        assertThat(pageable.getValue().getPageSize()).isEqualTo(5);
    }

    @Test
    void getSupplierById_WithExistingId_ReturnsSupplier() throws Exception {
        when(supplierService.getSupplierById(1L)).thenReturn(new LookupResponse(1L, null, "Northfield Kennels"));

        mockMvc.perform(get("/api/dogs/suppliers/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Northfield Kennels"));
    }

    @Test
    void getSupplierById_WithUnknownId_ReturnsNotFound() throws Exception {
        when(supplierService.getSupplierById(99L)).thenThrow(new ResourceNotFoundException("Supplier", 99L));

        mockMvc.perform(get("/api/dogs/suppliers/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").exists());
    }

    @Test
    void createSupplier_WithValidRequest_ReturnsCreated() throws Exception {
        when(supplierService.createSupplier(new LookupRequest(null, "Oakridge Kennels"))).thenReturn(new LookupResponse(5L, null, "Oakridge Kennels"));

        mockMvc.perform(post("/api/dogs/suppliers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Oakridge Kennels"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.name").value("Oakridge Kennels"));
    }

    @Test
    void createSupplier_WithBlankName_ReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/dogs/suppliers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": " "}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").exists());

        verifyNoInteractions(supplierService);
    }

    @Test
    void updateSupplierById_WithValidRequest_ReturnsUpdatedSupplier() throws Exception {
        when(supplierService.updateSupplierById(1L, new LookupRequest(null, "Brookvale Kennels")))
                .thenReturn(new LookupResponse(1L, null, "Brookvale Kennels"));

        mockMvc.perform(put("/api/dogs/suppliers/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Brookvale Kennels"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Brookvale Kennels"));
    }

    @Test
    void deleteSupplierById_WithExistingId_ReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/dogs/suppliers/1"))
                .andExpect(status().isNoContent());

        verify(supplierService).deleteSupplierById(1L);
    }
}
