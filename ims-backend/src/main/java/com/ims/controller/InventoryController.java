package com.ims.controller;

import com.ims.dto.request.InventoryRequest;
import com.ims.dto.request.SellRequest;
import com.ims.dto.response.ApiResponse;
import com.ims.dto.response.InventoryResponse;
import com.ims.service.impl.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
@Tag(name = "Inventory", description = "Inventory receive/sell and stock management APIs")
@SecurityRequirement(name = "bearerAuth")
public class InventoryController {

    private final InventoryService inventoryService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE_OPERATOR')")
    @Operation(summary = "Create inventory record (ADMIN/WAREHOUSE_OPERATOR)")
    public ResponseEntity<ApiResponse<InventoryResponse>> create(@Valid @RequestBody InventoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Inventory created", inventoryService.create(request)));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE_OPERATOR')")
    @Operation(summary = "List inventory items")
    public ResponseEntity<ApiResponse<List<InventoryResponse>>> getAll(
        @RequestParam(required = false) String productName) {
        return ResponseEntity.ok(ApiResponse.success(inventoryService.getAll(productName)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE_OPERATOR')")
    @Operation(summary = "Get inventory by ID")
    public ResponseEntity<ApiResponse<InventoryResponse>> getById(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(inventoryService.getById(id)));
    }

    @GetMapping("/product/{productId}")
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE_OPERATOR')")
    @Operation(summary = "Get inventory by product")
    public ResponseEntity<ApiResponse<InventoryResponse>> getByProduct(@PathVariable String productId) {
        return ResponseEntity.ok(ApiResponse.success(inventoryService.getByProduct(productId)));
    }

    @GetMapping("/low-stock")
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE_OPERATOR')")
    @Operation(summary = "Get low-stock products (ADMIN/WAREHOUSE_OPERATOR)")
    public ResponseEntity<ApiResponse<List<InventoryResponse>>> getLowStock() {
        return ResponseEntity.ok(ApiResponse.success(inventoryService.getLowStockItems()));
    }

    @PatchMapping("/{id}/restock")
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE_OPERATOR')")
    @Operation(summary = "Restock inventory item by ID")
    public ResponseEntity<ApiResponse<InventoryResponse>> restock(
        @PathVariable String id,
        @RequestParam @Min(1) int quantity) {
        return ResponseEntity.ok(ApiResponse.success("Stock updated", inventoryService.restock(id, quantity)));
    }

    @PatchMapping("/product/{productId}/selling-price")
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE_OPERATOR')")
    @Operation(summary = "Update selling price for product inventory")
    public ResponseEntity<ApiResponse<InventoryResponse>> updateSellingPrice(
        @PathVariable String productId,
        @RequestParam("value") @DecimalMin("0.01") BigDecimal sellingPrice) {
        return ResponseEntity.ok(ApiResponse.success(
            "Selling price updated",
            inventoryService.updateSellingPrice(productId, sellingPrice)
        ));
    }

    @PostMapping("/sell")
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE_OPERATOR')")
    @Operation(summary = "Sell stock to customer and reduce quantity")
    public ResponseEntity<ApiResponse<InventoryResponse>> sell(@Valid @RequestBody SellRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Sale recorded", inventoryService.sell(request)));
    }
}
