package com.ims.controller;

import com.ims.dto.request.ProductRequest;
import com.ims.dto.request.VendorPriceUpdateRequest;
import com.ims.dto.request.VendorProductRequest;
import com.ims.dto.response.ApiResponse;
import com.ims.dto.response.ProductResponse;
import com.ims.service.impl.ProductService;
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
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
@Tag(name = "Products", description = "Product catalog and vendor supply APIs")
public class ProductController {

    private final ProductService productService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create product catalog entry (ADMIN only)", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<ProductResponse>> create(@Valid @RequestBody ProductRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Product created", productService.create(request)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get product by ID")
    public ResponseEntity<ApiResponse<ProductResponse>> getById(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(productService.getById(id)));
    }

    @GetMapping
    @Operation(summary = "List products with filters: category, vendorId, name")
    public ResponseEntity<ApiResponse<Page<ProductResponse>>> getAll(
        @RequestParam(required = false) String category,
        @RequestParam(required = false) String vendorId,
        @RequestParam(required = false) String name,
        @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(productService.getAll(category, vendorId, name, pageable)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update product catalog entry (ADMIN only)", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<ProductResponse>> update(
        @PathVariable String id,
        @Valid @RequestBody ProductRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Product updated", productService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete product (ADMIN only)", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String id) {
        productService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Product deleted", null));
    }

    @GetMapping("/vendor/my")
    @PreAuthorize("hasRole('VENDOR')")
    @Operation(summary = "List products supplied by current vendor", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<Page<ProductResponse>>> getMySupplies(
        @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(productService.getCurrentVendorProducts(pageable)));
    }

    @PostMapping("/vendor/my/supplies")
    @PreAuthorize("hasRole('VENDOR')")
    @Operation(summary = "Add product supply with vendor-specific price", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<ProductResponse>> addMySupply(@Valid @RequestBody VendorProductRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Supply added", productService.addSupplyForCurrentVendor(request)));
    }

    @PatchMapping("/{productId}/vendor/my/price")
    @PreAuthorize("hasRole('VENDOR')")
    @Operation(summary = "Update current vendor price for a product", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<ProductResponse>> updateMyVendorPrice(
        @PathVariable String productId,
        @Valid @RequestBody VendorPriceUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
            "Vendor price updated",
            productService.updateCurrentVendorPrice(productId, request.getVendorPrice())
        ));
    }
}
