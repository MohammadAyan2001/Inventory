package com.ims.controller;

import com.ims.dto.request.VendorRequest;
import com.ims.dto.response.ApiResponse;
import com.ims.entity.Vendor;
import com.ims.service.impl.VendorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/vendors")
@RequiredArgsConstructor
@Tag(name = "Vendors", description = "Vendor management APIs")
@SecurityRequirement(name = "bearerAuth")
public class VendorController {

    private final VendorService vendorService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create vendor (ADMIN only)")
    public ResponseEntity<ApiResponse<Vendor>> create(@Valid @RequestBody VendorRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Vendor created", vendorService.create(request)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get vendor by ID")
    public ResponseEntity<ApiResponse<Vendor>> getById(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(vendorService.getById(id)));
    }

    @GetMapping
    @Operation(summary = "List vendors with optional name filter")
    public ResponseEntity<ApiResponse<Page<Vendor>>> getAll(
        @RequestParam(required = false) String name,
        @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(vendorService.getAll(name, pageable)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update vendor (ADMIN only)")
    public ResponseEntity<ApiResponse<Vendor>> update(@PathVariable String id, @Valid @RequestBody VendorRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Vendor updated", vendorService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete vendor (ADMIN only)")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String id) {
        vendorService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Vendor deleted", null));
    }
}
