package com.ims.controller;

import com.ims.dto.request.OrderRequest;
import com.ims.dto.request.ReceiveOrderRequest;
import com.ims.dto.response.ApiResponse;
import com.ims.dto.response.OrderResponse;
import com.ims.enums.OrderStatus;
import com.ims.service.impl.OrderService;
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
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Tag(name = "Orders", description = "Purchase order lifecycle APIs")
@SecurityRequirement(name = "bearerAuth")
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE_OPERATOR')")
    @Operation(summary = "Create purchase order to vendor")
    public ResponseEntity<ApiResponse<OrderResponse>> placeOrder(@Valid @RequestBody OrderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Order created", orderService.placeOrder(request)));
    }

    @GetMapping("/{reference}")
    @Operation(summary = "Get order by reference")
    public ResponseEntity<ApiResponse<OrderResponse>> getByReference(@PathVariable String reference) {
        return ResponseEntity.ok(ApiResponse.success(orderService.getByReference(reference)));
    }

    @GetMapping("/my")
    @Operation(summary = "Get current user's created orders")
    public ResponseEntity<ApiResponse<Page<OrderResponse>>> getMyOrders(
        @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(orderService.getMyOrders(pageable)));
    }

    @GetMapping("/vendor/incoming")
    @PreAuthorize("hasRole('VENDOR')")
    @Operation(summary = "Get current vendor incoming purchase orders")
    public ResponseEntity<ApiResponse<Page<OrderResponse>>> getVendorIncomingOrders(
        @RequestParam(required = false) OrderStatus status,
        @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(orderService.getVendorIncomingOrders(status, pageable)));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE_OPERATOR')")
    @Operation(summary = "Get all purchase orders (ADMIN/WAREHOUSE_OPERATOR)")
    public ResponseEntity<ApiResponse<Page<OrderResponse>>> getAllOrders(
        @RequestParam(required = false) OrderStatus status,
        @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(orderService.getAllOrders(status, pageable)));
    }

    @PatchMapping("/{reference}/accept")
    @PreAuthorize("hasRole('VENDOR')")
    @Operation(summary = "Vendor accepts order")
    public ResponseEntity<ApiResponse<OrderResponse>> acceptOrder(@PathVariable String reference) {
        return ResponseEntity.ok(ApiResponse.success("Order accepted", orderService.acceptOrder(reference)));
    }

    @PatchMapping("/{reference}/ship")
    @PreAuthorize("hasRole('VENDOR')")
    @Operation(summary = "Vendor marks order as shipped")
    public ResponseEntity<ApiResponse<OrderResponse>> shipOrder(@PathVariable String reference) {
        return ResponseEntity.ok(ApiResponse.success("Order shipped", orderService.shipOrder(reference)));
    }

    @PatchMapping("/{reference}/receive")
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE_OPERATOR')")
    @Operation(summary = "Warehouse confirms order receipt and updates inventory")
    public ResponseEntity<ApiResponse<OrderResponse>> receiveOrder(
        @PathVariable String reference,
        @Valid @RequestBody ReceiveOrderRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Order received", orderService.receiveOrder(reference, request)));
    }

    @PatchMapping("/{reference}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE_OPERATOR')")
    @Operation(summary = "Cancel order (ADMIN/WAREHOUSE_OPERATOR)")
    public ResponseEntity<ApiResponse<OrderResponse>> cancelOrder(@PathVariable String reference) {
        return ResponseEntity.ok(ApiResponse.success("Order cancelled", orderService.cancelOrder(reference)));
    }
}
